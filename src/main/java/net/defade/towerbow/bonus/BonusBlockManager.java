package net.defade.towerbow.bonus;

import io.github.togar2.pvp.projectile.AbstractArrow;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

public class BonusBlockManager implements BlockHandler {
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
        if (gameInstance.getWorldAge() % 30 == 0) {
            Block block = tick.getBlock();
            gameInstance.setBlock(tick.getBlockPosition(), getRandomBlock(block)
                    .withHandler(this)
                    .withNbt(block.nbt())
            );
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

                shooter.sendMessage(Component.text("Vous avez obtenu un bonus de type " + block.getTag(BONUS_BLOCK_TAG) + "!"));
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
