package net.defade.towerbow.fight;

import io.github.togar2.pvp.config.ArmorToolConfig;
import io.github.togar2.pvp.config.AttackConfig;
import io.github.togar2.pvp.config.DamageConfig;
import io.github.togar2.pvp.config.FoodConfig;
import io.github.togar2.pvp.config.PotionConfig;
import io.github.togar2.pvp.config.ProjectileConfig;
import io.github.togar2.pvp.config.PvPConfig;
import io.github.togar2.pvp.projectile.AbstractArrow;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
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
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;
import java.util.UUID;

public class CombatMechanics {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Integer> PLAYER_KILLS = Tag.Integer("kills");
    private static final Tag<Integer> PLAYER_LONGSHOTS = Tag.Integer("longshots");
    private static final Tag<Integer> PLAYER_DAMAGE_DEALT = Tag.Integer("damage_dealt");
    public static final Tag<Integer> PLAYER_REMAINING_LIVES = Tag.Integer("player_remaining_lives");
    private static final Tag<UUID> LAST_DAMAGER_UUID = Tag.UUID("last_damager"); // Used to store the last player who damaged the player

    private static final Tag<Pos> PLAYER_SHOOT_POS = Tag.Structure("arrow_touched_ground", Pos.class); // Position at which the player shot the arrow
    private static final Tag<Boolean> ARROW_TOUCHED_GROUND = Tag.Boolean("arrow_touched_ground"); // Used to check if the arrow can be considered a longshot

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
        return PvPConfig.emptyBuilder()
                // Enable all PvP features
                .attack(AttackConfig.defaultBuilder().attackCooldown(false).build())
                .armorTool(ArmorToolConfig.DEFAULT)
                .damage(DamageConfig.defaultBuilder().deathMessages(false)) // We have our own
                .food(FoodConfig.emptyBuilder(false)
                        .eating(true)
                        .eatingSounds(true)
                        .naturalRegeneration(true)
                )
                .projectile(ProjectileConfig.emptyBuilder(false)
                        .bow(true)
                )
                .potion(PotionConfig.emptyBuilder(false)
                        .applyEffect(true)
                        .updateEffect(true)
                )
                .build()
                .createNode();
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
            }
        });
    }

    private void registerLongShots(GameInstance gameInstance) {
        combatMechanicsNode
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    entityShootEvent.getProjectile().setTag(PLAYER_SHOOT_POS, entityShootEvent.getEntity().getPosition());
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    projectileCollideWithBlockEvent.getEntity().setTag(ARROW_TOUCHED_GROUND, true);
                })
                .addListener(EntityDamageEvent.class, entityDamageEvent -> {
                    if (!(entityDamageEvent.getDamage().getSource() instanceof AbstractArrow arrow)) return;

                    if (arrow.hasTag(ARROW_TOUCHED_GROUND) && arrow.getTag(ARROW_TOUCHED_GROUND)) {
                        return; // Arrow touched the ground, can't be considered a longshot
                    }

                    Player shooter = (Player) arrow.getShooter();
                    Player target = (Player) entityDamageEvent.getEntity();

                    if (shooter == null || !arrow.hasTag(PLAYER_SHOOT_POS))
                        return; // Should never happen but just in case

                    shooter.setTag(PLAYER_DAMAGE_DEALT, getDamageDealt(shooter) + (int) entityDamageEvent.getDamage().getAmount());

                    Pos shootPos = arrow.getTag(PLAYER_SHOOT_POS);

                    double distance = shootPos.distance(target.getPosition());
                    shooter.setLevel((int) distance); // Distance displayed in xp bar
                    if (distance > 50) { // If the distance is > 50 blocks then it's a longshot
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
            if (killer == null || killer == playerDeathEvent.getPlayer()) return; // Ignore self kills

            killer.setTag(PLAYER_KILLS, getKills(killer) + 1);
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
            playerDeathEvent.setChatMessage(killText);

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

                deadPlayer.getTeam().getPlayers().forEach(players -> {
                    players.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(2000),Duration.ofMillis(500)));
                    players.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                    players.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Plus qu'une vie!</red>"));

                    players.sendMessage(MM.deserialize(
                            "<dark_red><b>ATTENTION!</b><dark_red> <red>Votre dernier allié est mort, il ne vous reste plus qu'une vie!</red>"
                    ));
                });

                gameInstance.getPlayers().forEach(players -> players.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_DRAGON_GROWL).pitch(1.6F).volume(0.5F).build(), players.getPosition()));
            } else { // Reviving the player if he isn't the last player on his team, otherwise he is automatically a final kill
                revivePlayer(deadPlayer, getRemainingLives(deadPlayer));

                deadPlayer.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(2000),Duration.ofMillis(500)));
                deadPlayer.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>MORT!</b></dark_red>"));
                deadPlayer.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>-1 ❤</red>"));

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
        }).addListener(PlayerDisconnectEvent.class, playerDisconnectEvent -> {
            Player player = playerDisconnectEvent.getPlayer();
            if (player.getGameMode() == GameMode.SPECTATOR) return;

            PlayerDeathEvent playerDeathEvent = new PlayerDeathEvent(player, null, null);
            combatMechanicsNode.call(playerDeathEvent);
        });
    }

    public static void revivePlayer(Player player, int lives) {
        player.setTag(PLAYER_REMAINING_LIVES, lives);

        Pos respawnPosition = player.getTeam().getPlayers().stream()
                .filter(playerPredicate -> playerPredicate != player)
                .findFirst().get().getPosition();

        // Teleports the player to an alive ally.
        player.setRespawnPoint(respawnPosition);
        player.teleport(respawnPosition);

        player.setHealth(20);
        player.setGameMode(GameMode.SURVIVAL);
        player.setCanPickupItem(true);
        player.setInvisible(false);

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
