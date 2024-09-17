package net.defade.towerbow.bonus;

import io.github.togar2.pvp.entity.projectile.AbstractArrow;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.GamePlayHandler;
import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BonusBlockManager implements BlockHandler {
    private static final int MIN_DISTANCE_BETWEEN_PLAYERS = 20;
    private static final Tag<String> BONUS_BLOCK_TAG = Tag.String("bonus_block");
    private static final Tag<Integer> PLAYER_BONUS_BLOCK_COUNT = Tag.Integer("bonus_block_count");

    private static final Block[] WOOL_BLOCKS = new Block[] {
            Block.WHITE_WOOL,
            Block.ORANGE_WOOL,
            Block.MAGENTA_WOOL,
            Block.LIGHT_BLUE_WOOL,
            Block.YELLOW_WOOL,
            Block.LIME_WOOL,
            Block.PINK_WOOL,
            Block.GRAY_WOOL,
            Block.LIGHT_GRAY_WOOL,
            Block.CYAN_WOOL,
            Block.PURPLE_WOOL,
            Block.BLUE_WOOL,
            Block.BROWN_WOOL,
            Block.GREEN_WOOL,
            Block.RED_WOOL,
            Block.BLACK_WOOL
    };

    private static final Map<String, BonusBlock> bonusBlocks = Map.of(
            "explosive_arrow", new ExplosiveArrowBonusBlock(),
            "smoke_arrow", new SmokeArrowBonusBlock(),
            "heal_bonus", new HealBonusBlock(),
            "strike_bonus", new StrikeBonusBlock(),
            "wall_arrow", new WallArrowBonusBlock()
    );

    private static final Random RANDOM = new Random();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GameInstance gameInstance;
    private final GameEventNode gameEventNode;

    public BonusBlockManager(GameInstance gameInstance, GamePlayHandler gamePlayHandler) {
        this.gameInstance = gameInstance;
        this.gameEventNode = gamePlayHandler.getGameEventNode();

        bonusBlocks.values().forEach(bonusBlock -> bonusBlock.registerMechanics(gameInstance));
        registerBlockHitListener();
    }

    public void spawnBonusBlock() {
        // Wrapped with ArrayList because .toList returns an immutable list
        List<Player> firstTeamPlayers = new ArrayList<>(gameInstance.getTeams().firstTeam().getPlayers().stream().toList());
        List<Player> secondTeamPlayers = new ArrayList<>(gameInstance.getTeams().secondTeam().getPlayers().stream().toList());
        Collections.shuffle(firstTeamPlayers);
        Collections.shuffle(secondTeamPlayers);

        Pos spawnPosition = null;
        for (Player firstTeamPlayer : firstTeamPlayers) {
            for (Player secondTeamPlayer : secondTeamPlayers) {
                if (firstTeamPlayer.getPosition().distanceSquared(secondTeamPlayer.getPosition()) < MIN_DISTANCE_BETWEEN_PLAYERS * MIN_DISTANCE_BETWEEN_PLAYERS) {
                    continue;
                }

                // Get the middle point between the two players and randomize the position a bit
                spawnPosition = firstTeamPlayer.getPosition().add(secondTeamPlayer.getPosition()).div(2)
                        .add(RANDOM.nextInt(-7, 7), RANDOM.nextInt(-7, 7), RANDOM.nextInt(-7, 7));
            }
        }

        if (spawnPosition == null) {
            // Set the position 10 blocks under a random player
            Player randomPlayer = gameInstance.getAlivePlayers().stream()
                    .skip(RANDOM.nextInt(gameInstance.getAlivePlayers().size()))
                    .findFirst()
                    .orElseThrow();

            spawnPosition = randomPlayer.getPosition().add(0, 10, 0);
        }

        String bonusBlockId = bonusBlocks.keySet().stream()
                .skip((int) (bonusBlocks.size() * Math.random()))
                .findFirst()
                .orElseThrow();

        gameInstance.setBlock(spawnPosition, getRandomBlock(null)
                .withTag(BONUS_BLOCK_TAG, bonusBlockId)
                .withHandler(this)
        );
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NamespaceID.from("towerbow:bonus_block");
    }

    @Override
    public void tick(@NotNull BlockHandler.Tick tick) {
        if (gameInstance.getWorldAge() % 20 == 0) {
            Block block = tick.getBlock();
            gameInstance.setBlock(tick.getBlockPosition(), getRandomBlock(block)
                    .withHandler(this)
                    .withNbt(block.nbt())
            );
            gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_BEACON_AMBIENT).pitch(1F).volume(2F).build(), tick.getBlockPosition()); //Bonus bloc ambient sound

            gameInstance.sendGroupedPacket(new ParticlePacket(
                    Particle.END_ROD,
                    true,
                    tick.getBlockPosition().add(0.5,0.5,0.5),
                    new Vec(0, 0, 0),
                    0.1F,
                    20
            ));
            gameInstance.sendGroupedPacket(new ParticlePacket(
                    Particle.SOUL_FIRE_FLAME,
                    true,
                    tick.getBlockPosition().add(0.5,0.5,0.5),
                    new Vec(0.2, 0.2, 0.2),
                    0.05F,
                    20
            ));
        }
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    private void registerBlockHitListener() {
        gameEventNode.getEntityInstanceNode().addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
            if(!(projectileCollideWithBlockEvent.getEntity() instanceof AbstractArrow arrow)) return;
            if (arrow.getShooter() == null) return;
            Player shooter = (Player) arrow.getShooter();

            Block block = projectileCollideWithBlockEvent.getBlock();
            if (!block.hasTag(BONUS_BLOCK_TAG)) {
                return;
            }

            BonusBlock bonusBlock = bonusBlocks.get(block.getTag(BONUS_BLOCK_TAG));

            if (bonusBlock != null) {
                bonusBlock.onHit(shooter);

                shooter.setTag(PLAYER_BONUS_BLOCK_COUNT, getBonusBlockCount(shooter) + 1);

                gameInstance.setBlock(projectileCollideWithBlockEvent.getCollisionPosition(), Block.AIR);
                gameInstance.sendGroupedPacket(new ParticlePacket(
                        Particle.FIREWORK,
                        true,
                        projectileCollideWithBlockEvent.getCollisionPosition().add(0.5,0.5,0.5),
                        new Vec(0, 0, 0),
                        0.25F,
                        75
                ));

                arrow.scheduleNextTick(Entity::remove);

                if (block.getTag(BONUS_BLOCK_TAG).equals("smoke_arrow") || block.getTag(BONUS_BLOCK_TAG).equals("explosive_arrow") || block.getTag(BONUS_BLOCK_TAG).equals("wall_arrow")) { // If the block is an arrow, show it to the shooter
                    shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(3500),Duration.ofMillis(500)));
                    shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                    shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<dark_gray>»</dark_gray> <b><red>"
                            + (block.getTag(BONUS_BLOCK_TAG)).replace("_"," ").toUpperCase() + "</red></b> <dark_gray>«</dark_gray>"));
                } else if (!block.getTag(BONUS_BLOCK_TAG).equals("heal_bonus")) {
                    shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1500),Duration.ofMillis(500)));
                    shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize(" "));
                    shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<light_purple>Vous recevez </light_purple><dark_purple><b>"
                            + (block.getTag(BONUS_BLOCK_TAG)).replace("_"," ").toUpperCase() + "</b></dark_purple><light_purple> !</light_purple>"));
                }

                gameInstance.getPlayers().forEach(player -> {
                    player.sendMessage(MM.deserialize(
                            "<dark_purple>\uD83C\uDFF9 <b>BLOC BONUS!</b></dark_purple> <shooter> <light_purple>a reçu </light_purple><dark_purple>"
                                    + (block.getTag(BONUS_BLOCK_TAG)).replace("_"," ").toUpperCase() + "</dark_purple><light_purple> !</light_purple>",
                            Placeholder.component("shooter", shooter.getName())

                    ));
                    if (shooter.getTeam() == player.getTeam()) { // An ally shot the bonus block
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_END_PORTAL_FRAME_FILL).pitch(0.7F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_BEACON_ACTIVATE).pitch(1.2F).volume(1F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_VAULT_OPEN_SHUTTER).pitch(0.7F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_BELL).pitch(1F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_BELL).pitch(0F).volume(0.5F).build(), player.getPosition());
                    } else { // An enemy shot the bonus block
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_END_PORTAL_FRAME_FILL).pitch(0.7F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_RESPAWN_ANCHOR_DEPLETE).pitch(1F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_VAULT_BREAK).pitch(0F).volume(0.5F).build(), player.getPosition());
                        player.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_AMBIENT_OMINOUS).pitch(1F).volume(0.5F).build(), player.getPosition());
                    }
                });
            }
        });
    }

    private Block getRandomBlock(Block excludedBlock) {
        Block randomBlock = excludedBlock;

        while (randomBlock == excludedBlock) {
            randomBlock = WOOL_BLOCKS[(int) (Math.random() * WOOL_BLOCKS.length)];
        }

        return randomBlock;
    }

    public static int getBonusBlockCount(Player player) {
        return player.hasTag(PLAYER_BONUS_BLOCK_COUNT) ? player.getTag(PLAYER_BONUS_BLOCK_COUNT) : 0;
    }


    public static Map<String, BonusBlock> getBonusBlocks() {
        return bonusBlocks;
    }

    public static BonusBlock getBonusBlock(String id) {
        return bonusBlocks.get(id);
    }
}
