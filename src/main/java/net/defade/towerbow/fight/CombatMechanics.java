package net.defade.towerbow.fight;

import io.github.togar2.pvp.entity.projectile.AbstractArrow;
import io.github.togar2.pvp.feature.CombatFeatureSet;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.utils.CombatVersion;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.utils.Items;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class CombatMechanics {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Integer> PLAYER_KILLS = Tag.Integer("kills");
    private static final Tag<Integer> PLAYER_LONGSHOTS = Tag.Integer("longshots");
    private static final Tag<Integer> PLAYER_DAMAGE_DEALT = Tag.Integer("damage_dealt");
    private static final Tag<Integer> PLAYER_REMAINING_LIVES = Tag.Integer("player_remaining_lives");
    private static final Tag<UUID> LAST_DAMAGER_UUID = Tag.UUID("last_damager"); // Used to store the last player who damaged the player

    private static final Tag<Pos> PLAYER_SHOOT_POS = Tag.Structure("arrow_touched_ground", Pos.class); // Position at which the player shot the arrow
    private static final Tag<Boolean> ARROW_TOUCHED_GROUND = Tag.Boolean("arrow_touched_ground"); // Used to check if the arrow can be considered a longshot

    private static final AttributeModifier FREEZE_PLAYER_MODIFIER = new AttributeModifier(NamespaceID.from("defade:freeze_player"), -10000, AttributeOperation.ADD_VALUE);

    private static final CombatFeatureSet COMBAT_FEATURES = CombatFeatures.empty()
            .version(CombatVersion.MODERN)
            .add(CombatFeatures.VANILLA_ENCHANTMENT)
            .add(CombatFeatures.VANILLA_ATTACK)
            .add(CombatFeatures.VANILLA_EXHAUSTION)
            .add(CombatFeatures.VANILLA_CRITICAL)
            .add(CombatFeatures.VANILLA_SWEEPING)
            .add(CombatFeatures.VANILLA_KNOCKBACK)

            .add(CombatFeatures.VANILLA_EQUIPMENT)
            .add(CombatFeatures.VANILLA_ITEM_COOLDOWN)

            .add(CombatFeatures.VANILLA_DAMAGE)
            .add(CombatFeatures.VANILLA_BLOCK)
            .add(CombatFeatures.VANILLA_ARMOR)
            .add(CombatFeatures.VANILLA_PLAYER_STATE)
            .add(CombatFeatures.VANILLA_TOTEM)
            .add(CombatFeatures.VANILLA_ITEM_DAMAGE)
            .add(CombatFeatures.VANILLA_FALL)

            .add(CombatFeatures.VANILLA_FOOD)
            .add(CombatFeatures.VANILLA_REGENERATION)

            .add(CombatFeatures.VANILLA_BOW)
            .add(CombatFeatures.VANILLA_PROJECTILE_ITEM)

            .add(CombatFeatures.VANILLA_EFFECT)
            .add(CombatFeatures.VANILLA_POTION)

            .build();

    private final EventNode<EntityInstanceEvent> combatMechanicsNode;

    private CombatMechanics(GameInstance gameInstance) {
        this.combatMechanicsNode = createPvPNode();
        disableArrowSpread();
        disableFriendlyFire();

        registerLongShots(gameInstance);

        registerKillCounter();
        registerDeathHandler();
    }

    private EventNode<EntityInstanceEvent> createPvPNode() {
        return COMBAT_FEATURES.createNode();
    }

    private void disableArrowSpread() {
        combatMechanicsNode.addListener(EntityShootEvent.class, entityShootEvent -> {
            entityShootEvent.setSpread(0); // Disable arrow randomness
        });
    }

    private void disableFriendlyFire() {
        combatMechanicsNode.addListener(EntityDamageEvent.class, entityDamageEvent -> {
            if (!(entityDamageEvent.getEntity() instanceof Player player)) return;

            Damage damageSource = entityDamageEvent.getDamage();
            Player damager;
            if (damageSource.getSource() instanceof Player) {
                damager = (Player) damageSource.getSource();
            } else if (damageSource.getSource() instanceof AbstractArrow arrow) {
                damager = (Player) arrow.getShooter();
            } else {
                return;
            }

            if (player == damager) return; // Allow self damage

            if (player.getTeam() == damager.getTeam()) {
                entityDamageEvent.setCancelled(true);
                if (damageSource.getSource() instanceof AbstractArrow arrow) {
                    arrow.scheduleNextTick(Entity::remove);
                }
            }
        });
    }

    private void registerLongShots(GameInstance gameInstance) {
        combatMechanicsNode
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    entityShootEvent.getProjectile().setTag(PLAYER_SHOOT_POS, entityShootEvent.getEntity().getPosition());
                    gameInstance.getEntities().stream().filter(arrow -> arrow.getEntityType().equals(EntityType.ARROW)).forEach(arrow -> {
                        if(arrow.getAliveTicks() > 15 * 20 && arrow.hasTag(ARROW_TOUCHED_GROUND)) { // Delete any old arrows on blocks
                            arrow.scheduleNextTick(Entity::remove);
                        }
                    });
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    Entity arrow = projectileCollideWithBlockEvent.getEntity();
                    Pos blockPos = projectileCollideWithBlockEvent.getCollisionPosition();

                    if (arrow.hasTag(ARROW_TOUCHED_GROUND)) return;

                    if (projectileCollideWithBlockEvent.getBlock() == Block.COBBLESTONE && (int) (Math.random() * 100) >= 50) { //50% chance [CONFIG: Arrow cobblestone breaking chance (%)]
                        gameInstance.setBlock(blockPos, Block.MOSSY_COBBLESTONE);
                        gameInstance.getWorldHandler().registerBlockDecay(blockPos, 15 * 1000); // 15 seconds
                    } else if (projectileCollideWithBlockEvent.getBlock() == Block.MOSSY_COBBLESTONE) {
                        gameInstance.setBlock(blockPos, Block.MOSSY_COBBLESTONE);
                        gameInstance.getWorldHandler().registerBlockDecay(blockPos, 10); // 15 seconds
                    }

                    arrow.setTag(ARROW_TOUCHED_GROUND, true);
                })
                .addListener(EntityDamageEvent.class, entityDamageEvent -> {
                    if (!(entityDamageEvent.getDamage().getSource() instanceof AbstractArrow arrow)) return;

                    if (arrow.hasTag(ARROW_TOUCHED_GROUND) && arrow.getTag(ARROW_TOUCHED_GROUND)) {
                        entityDamageEvent.setCancelled(true); //Cancel every damage from arrow that touched a block
                        entityDamageEvent.getDamage().getSource().scheduleNextTick(Entity::remove);
                        return; // Arrow touched the ground, can't be considered a longshot
                    }

                    Player shooter = (Player) arrow.getShooter();
                    Player target = (Player) entityDamageEvent.getEntity();

                    if (shooter == null || !arrow.hasTag(PLAYER_SHOOT_POS))
                        return; // Should never happen but just in case

                    if (shooter == target) return;
                    gameInstance.scheduler().scheduleNextTick(() -> shooter.sendActionBar(generateHealthBar(target)));

                    shooter.setTag(PLAYER_DAMAGE_DEALT, getDamageDealt(shooter) + (int) entityDamageEvent.getDamage().getAmount());

                    Pos shootPos = arrow.getTag(PLAYER_SHOOT_POS);

                    double distance = shootPos.distance(target.getPosition());
                    shooter.setLevel((int) distance); // Distance displayed in xp bar

                    double minDistanceForLongshot = Math.max(40, gameInstance.getWorldBorder().diameter() * 0.8); //scales the longshot distance with the map size, minimum 40blocks

                    if (distance > minDistanceForLongshot) {
                        shooter.setTag(PLAYER_LONGSHOTS, getLongshotCount(shooter) + 1);

                        shooter.setHealth(Math.min(shooter.getHealth() + 3, (float) shooter.getAttributeValue(Attribute.GENERIC_MAX_HEALTH)));

                        // Longshot sound & message
                        shooter.getInstance().getPlayers().forEach(players -> {
                            shooter.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_EYE_DEATH).pitch(1F).volume(2F).build(), shooter.getPosition());
                            players.playSound(Sound.sound().type(SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).pitch(1F).volume(1.5F).build(), players.getPosition());
                            players.sendMessage(MM.deserialize(
                                    "<gold>\uD83C\uDFF9 <b>LONGSHOT!</b></gold> <shooter> <yellow>a fait un longshot de <gold><b>"
                                            + Math.floor(distance * 10) / 10 + "</b> blocks</gold>!</yellow>",
                                    Placeholder.component("shooter", shooter.getName())
                            ));
                            shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(500),Duration.ofMillis(500)));
                            shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize("<gold><b>LONGSHOT!</b></gold>"));
                            shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>+2❤</red>"));
                        });
                        shooter.setExp(1); //Fill xp bar
                    } else {
                        shooter.setHealth(shooter.getHealth() + 1); // heal the shooter a bit
                        shooter.setExp(0); // Empty xp bar
                    }
                });
    }

    private void registerKillCounter() {
        combatMechanicsNode.addListener(EntityDamageEvent.class, entityDamageEvent -> {
            if (entityDamageEvent.getDamage().getAttacker() == null) return;
            entityDamageEvent.getEntity().setTag(LAST_DAMAGER_UUID, entityDamageEvent.getDamage().getAttacker().getUuid());
        }).addListener(PlayerDeathEvent.class, playerDeathEvent -> {
            Player killer = getLatestDamager(playerDeathEvent.getPlayer());
            Player killed = playerDeathEvent.getPlayer();
            if (killer == null || killer == playerDeathEvent.getPlayer()) return; // Ignore self kills

            killer.setTag(PLAYER_KILLS, getKills(killer) + 1);
            killer.getInventory().addItemStack(Items.GOLDEN_APPLE);

            killer.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1500),Duration.ofMillis(500)));
            killer.sendTitlePart(TitlePart.TITLE, MM.deserialize(" "));

            if (getRemainingLives(killed) > 1) {
                killer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<green><b>KILL!</b></green>"));
            } else {
                killer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<aqua><b>FINAL KILL!</b></aqua>"));
                killer.playSound(Sound.sound().type(SoundEvent.ENTITY_WITHER_SPAWN).pitch(1.4F).volume(0.5F).build(), killer.getPosition());
            }


        });
    }

    private void registerDeathHandler() {
        combatMechanicsNode.addListener(PlayerDeathEvent.class, playerDeathEvent -> {
            GameInstance gameInstance = (GameInstance) playerDeathEvent.getPlayer().getInstance();
            Player deadPlayer = playerDeathEvent.getPlayer();

            deadPlayer.setTag(PLAYER_REMAINING_LIVES, getRemainingLives(deadPlayer) - 1);
            if (deadPlayer.getTeam().getMembers().size() == 1) { //If he's alone, final kill him
                deadPlayer.setTag(PLAYER_REMAINING_LIVES, 0);
            }

            new Entity(EntityType.LIGHTNING_BOLT).setInstance(gameInstance, deadPlayer.getPosition());

            Component killText;
            Player killer = getLatestDamager(deadPlayer);
            if (killer == null) {
                killText = MM.deserialize(
                        "<red>\uD83C\uDFF9</red> <deadplayer> <yellow>est mort!</yellow>",
                        Placeholder.component("deadplayer", deadPlayer.getName())
                );
            } else {
                killText = MM.deserialize(
                        "<red>\uD83C\uDFF9</red> <deadplayer> <yellow>a été tué par</yellow> <killer><yellow>!</yellow>",
                        TagResolver.builder()
                                .resolver(Placeholder.component("deadplayer", deadPlayer.getName()))
                                .resolver(Placeholder.component("killer", killer.getName()))
                                .build()
                );
            }

            if (getRemainingLives(deadPlayer) <= 0) {
                killText = killText.append(MM.deserialize(
                        "<aqua><b> FINAL KILL!</b></aqua>"
                ));
            } else {
                killText = killText.append(MM.deserialize(
                        " <gray>(</gray><dark_red><b><liveremaining></b><dark_red> <red>Vies</red><gray>)</gray>",
                        Placeholder.component("liveremaining", Component.text(getRemainingLives(deadPlayer)))
                ));
            }
            playerDeathEvent.setChatMessage(null); // Player death event sends the message to the WHOLE server
            gameInstance.sendMessage(killText);

            gameInstance.getPlayers().forEach(player -> {
                if (player.getTeam() == deadPlayer.getTeam()) { // An ally dies
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).pitch(0.7F).volume(1.3F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_BLAZE_DEATH).pitch(0.7F).volume(0.25F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_LIGHTNING_BOLT_THUNDER).pitch(1.4F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_RESPAWN_ANCHOR_DEPLETE).pitch(0F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ITEM_GOAT_HORN_SOUND_1).pitch(0.3F).volume(1.2F).build(), player.getPosition());

                } else { // An enemy dies
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).pitch(1.2F).volume(1.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_LIGHTNING_BOLT_THUNDER).pitch(1.4F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ITEM_GOAT_HORN_SOUND_1).pitch(1F).volume(1F).build(), player.getPosition());
                }
            });

            if (getRemainingLives(deadPlayer) <= 0) { // The player has no lives, he is a final kill
                deadPlayer.setRespawnPoint(deadPlayer.getPosition());
                deadPlayer.setGameMode(GameMode.SPECTATOR);
                deadPlayer.setCanPickupItem(false);
                deadPlayer.setInvisible(true); // Hide the deadPlayer

                //TODO Remove arrow tags when final-died

                deadPlayer.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(4000),Duration.ofMillis(500)));
                deadPlayer.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>MORT!</b></dark_red>"));
                deadPlayer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize(""));

                if (deadPlayer.getTeam().getMembers().size() == 1) {
                    deadPlayer.getTeam().getPlayers().forEach(lastPlayer -> {
                        lastPlayer.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500)));
                        lastPlayer.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                        lastPlayer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Plus qu'une vie!</red>"));

                        lastPlayer.sendMessage(MM.deserialize(
                                "<dark_red><b>ATTENTION!</b><dark_red> <red>Votre dernier allié est mort, il ne vous reste plus qu'une vie!</red>"
                        ));

                        lastPlayer.setTag(PLAYER_REMAINING_LIVES, 1);

                    });
                }

                gameInstance.getPlayers().forEach(players -> players.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_DRAGON_GROWL).pitch(1.6F).volume(0.5F).build(), players.getPosition()));
            } else { // Reviving the player if he isn't the last player on his team, otherwise he is automatically a final kill
                revivePlayer(deadPlayer, getRemainingLives(deadPlayer));

                deadPlayer.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(5000),Duration.ofMillis(500)));
                deadPlayer.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>MORT!</b></dark_red>"));

                if(getRemainingLives(deadPlayer) > 1) { // Typo : 1 vie, 2 vies
                    deadPlayer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red><lives> VIES RESTANTES</red>", Placeholder.component("lives", Component.text(getRemainingLives(deadPlayer)))));
                } else {
                    deadPlayer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red><lives> VIE RESTANTE</red>", Placeholder.component("lives", Component.text(getRemainingLives(deadPlayer)))));
                }

                return;
            }

            // Check if all the players of his team are dead
            boolean allPlayersInTeamDead = deadPlayer.getTeam().getMembers().isEmpty();

            if (allPlayersInTeamDead) {
                Team playerTeam = deadPlayer.getTeam();
                Team opposingTeam = gameInstance.getTeams().firstTeam() == playerTeam
                        ? gameInstance.getTeams().secondTeam()
                        : gameInstance.getTeams().firstTeam();

                gameInstance.finishGame(opposingTeam, playerTeam);
            }
        }).addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            if (player.getGameMode() == GameMode.SPECTATOR) return;

            PlayerDeathEvent playerDeathEvent = new PlayerDeathEvent(player, null, null);
            combatMechanicsNode.call(playerDeathEvent);
        });
    }

    public static void revivePlayer(Player player, int lives) {
        GameInstance gameInstance = (GameInstance) player.getInstance();
        int horizontalBorderHeight = gameInstance.getGamePlayHandler().getHorizontalBorderHeight();
        player.setTag(PLAYER_REMAINING_LIVES, lives);

        Optional<Player> matchingPlayer = player.getTeam()
                .getPlayers()
                .stream()
                .filter(playerPredicate -> playerPredicate != player && playerPredicate.getPosition().y() > horizontalBorderHeight)
                .findAny();

        if (matchingPlayer.isPresent()) { // Teleports the player to a valid alive ally (= ally above the border)
            Player validAlly = matchingPlayer.get();

            player.setRespawnPoint(validAlly.getPosition());
            player.teleport(validAlly.getPosition());

            validAlly.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(3000),Duration.ofMillis(500)));
            validAlly.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
            validAlly.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<player> <gray>a revive sur vous !", Placeholder.component("player", player.getName())));
            validAlly.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDERMAN_HURT).pitch(0F).volume(1.5F).build(), validAlly.getPosition());
        } else { // Teleports the player to a random ally but above the border, so he doesn't die again
            Player randomAlly = player.getTeam().getPlayers().stream()
                    .filter(playerPredicate -> playerPredicate != player)
                    .findFirst().get();

            player.setRespawnPoint(randomAlly.getPosition().withY(horizontalBorderHeight + 10));
            player.teleport(randomAlly.getPosition().withY(horizontalBorderHeight + 10));
        }

        //Create a safe platform if the player is in the air
        int blockX = player.getPosition().blockX();
        int blockY = player.getPosition().blockY();
        int blockZ = player.getPosition().blockZ();

        for (int x = blockX - 1; x <= blockX + 1; x++) {
            for (int z = blockZ - 1; z <= blockZ + 1; z++) {
                Block blockType = gameInstance.getBlock(x, blockY - 1, z);
                if (blockType != Block.AIR) continue;

                gameInstance.setBlock(x, blockY - 1, z, Block.COBBLESTONE);
                gameInstance.getWorldHandler().registerBlockDecay(new Pos(x, blockY - 1, z), 15 * 1000); // 15 seconds
            }
        }

        player.setHealth(20);

        player.setGameMode(GameMode.SURVIVAL);
        player.setCanPickupItem(true);
        player.setInvulnerable(true);

        //Freeze the player for 4 sec
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(FREEZE_PLAYER_MODIFIER);
        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).addModifier(FREEZE_PLAYER_MODIFIER);
        player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(0);

        gameInstance.scheduler().scheduleTask(() -> player.addEffect(new Potion(PotionEffect.BLINDNESS, (byte) 1, 3 * 20)), TaskSchedule.millis(100), TaskSchedule.stop());

        // Unfreeze
        gameInstance.scheduler().scheduleTask(() -> {
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5);

            player.setInvulnerable(false);
            player.setFoodSaturation(0);
            player.setFood(20);
            player.getInventory().addItemStack(ItemStack.of(Material.GOLDEN_APPLE));

            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1500),Duration.ofMillis(500)));
            player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<green><b>RESPAWNED!</b></green>"));
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize(""));
            player.playSound(Sound.sound().type(SoundEvent.ENTITY_PLAYER_LEVELUP).pitch(0F).volume(0.5F).build(), player.getPosition());
        }, TaskSchedule.seconds(4), TaskSchedule.stop());

        player.sendPacket(new ParticlePacket(
                Particle.FLAME,
                true,
                player.getPosition().add(0,0.5,0),
                new Vec(0.2, 0.6, 0.2),
                0.5F,
                50
        ));
        player.sendPacket(new ParticlePacket(
                Particle.FIREWORK,
                true,
                player.getPosition().add(0,0.5,0),
                new Vec(0, 0, 0),
                0.3F,
                75
        ));
    }

    private Component generateHealthBar(Player player) {
        Component component = Component.text(player.getUsername() + " ").color(player.getTeam().getTeamColor());
        for (int i = 0; i < 10 + player.getAdditionalHearts() / 2 ; i++) {
            TextColor heartColor = (int) player.getHealth() / 2 > i ? TextColor.color(NamedTextColor.DARK_RED) : TextColor.color(26, 26, 26);
            if((int) player.getHealth()%2 == 1 && (int) player.getHealth()/2 == i) heartColor = TextColor.color(NamedTextColor.RED); // Half a heart = red
            if(i >= 10) heartColor = TextColor.color(NamedTextColor.YELLOW); // Absorption hearts

            component = component.append(Component.text("❤").color(heartColor));
        }
        component = component.append(Component.text(" " + (int) (player.getHealth() + player.getAdditionalHearts()) + "HP").color(NamedTextColor.RED));
        return component;
    }

    public static EventNode<EntityInstanceEvent> create(GameInstance gameInstance) {
        return new CombatMechanics(gameInstance).combatMechanicsNode;
    }

    public static int getKills(Player player) {
        return player.hasTag(PLAYER_KILLS) ? player.getTag(PLAYER_KILLS) : 0;
    }

    public static int getLongshotCount(Player player) {
        return player.hasTag(PLAYER_LONGSHOTS) ? player.getTag(PLAYER_LONGSHOTS) : 0;
    }

    public static int getDamageDealt(Player player) {
        return player.hasTag(PLAYER_DAMAGE_DEALT) ? player.getTag(PLAYER_DAMAGE_DEALT) : 0;
    }

    public static int getRemainingLives(Player player) {
        return player.hasTag(PLAYER_REMAINING_LIVES) ? player.getTag(PLAYER_REMAINING_LIVES) : 0;
    }

    public static void setRemainingLives(Player player, int lives) {
        player.setTag(PLAYER_REMAINING_LIVES, lives);
    }

    public static Player getLatestDamager(Player player) {
        UUID lastDamagerUUID = player.getTag(LAST_DAMAGER_UUID);
        return lastDamagerUUID == null ? null : player.getInstance().getPlayerByUuid(lastDamagerUUID);
    }

    public static boolean isAlive(Player player) {
        return player.getGameMode() != GameMode.SPECTATOR && player.isOnline();
    }

    public static boolean isDead(Player player) {
        return !isAlive(player);
    }
}
