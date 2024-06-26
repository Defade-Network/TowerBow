package net.defade.towerbow.bonus;

import io.github.togar2.pvp.projectile.AbstractArrow;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
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
import java.util.Map;

public class BonusBlockManager implements BlockHandler {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<String> BONUS_BLOCK_TAG = Tag.String("bonus_block");
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
            "poison_bonus", new PoisonBonusBlock()
    );

    private final GameInstance gameInstance;

    public BonusBlockManager(GameInstance gameInstance) {
        this.gameInstance = gameInstance;

        bonusBlocks.values().forEach(bonusBlock -> bonusBlock.registerMechanics(gameInstance));
        registerBlockHitListener();
    }

    public void spawnBonusBlock() {
        String bonusBlockId = bonusBlocks.keySet().stream()
                .skip((int) (bonusBlocks.size() * Math.random()))
                .findFirst()
                .orElseThrow();

        gameInstance.setBlock(10, 10, 10, getRandomBlock(null)
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
        gameInstance.getEventNode().getEntityInstanceNode().addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
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
                gameInstance.setBlock(projectileCollideWithBlockEvent.getCollisionPosition(), Block.AIR);
                gameInstance.sendGroupedPacket(new ParticlePacket(
                        Particle.FIREWORK,
                        true,
                        projectileCollideWithBlockEvent.getCollisionPosition().add(0.5,0.5,0.5),
                        new Vec(0, 0, 0),
                        0.25F,
                        75
                ));

                arrow.remove();

                if (block.getTag(BONUS_BLOCK_TAG).equals("smoke_arrow") || block.getTag(BONUS_BLOCK_TAG).equals("explosive_arrow")) { // If the block is an arrow, show it to the shooter
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
                            Placeholder.component("shooter", shooter.getName().color(TextColor.color(gameInstance.getTeams().getTeam(shooter).color())))

                    ));
                    if (gameInstance.getTeams().getTeam(shooter) == gameInstance.getTeams().getTeam(player)) { // An ally shot the bonus block
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
}
