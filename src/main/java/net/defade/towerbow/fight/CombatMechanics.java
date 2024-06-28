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
import net.defade.towerbow.teams.Team;
import net.defade.towerbow.teams.TeamsManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Pos;
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
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;
import java.util.UUID;

public class CombatMechanics {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Integer> PLAYER_KILLS = Tag.Integer("kills");
    private static final Tag<UUID> LAST_DAMAGER_UUID = Tag.UUID("last_damager"); // Used to store the last player who damaged the player

    private static final Tag<Pos> PLAYER_SHOOT_POS = Tag.Structure("arrow_touched_ground", Pos.class); // Position at which the player shot the arrow
    private static final Tag<Boolean> ARROW_TOUCHED_GROUND = Tag.Boolean("arrow_touched_ground"); // Used to check if the arrow can be considered a longshot

    private final EventNode<EntityInstanceEvent> combatMechanicsNode;

    private CombatMechanics(GameInstance gameInstance) {
        this.combatMechanicsNode = createPvPNode();
        disableArrowSpread();
        disableFriendlyFire();

        registerLongShots(gameInstance);

        registerDeathHandler();

        registerKillCounter();
    }

    private EventNode<EntityInstanceEvent> createPvPNode() {
        return PvPConfig.emptyBuilder()
                // Enable all PvP features
                .attack(AttackConfig.defaultBuilder().attackCooldown(false).build())
                .armorTool(ArmorToolConfig.DEFAULT)
                .damage(DamageConfig.DEFAULT)
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

            TeamsManager teamsManager = ((GameInstance) player.getInstance()).getTeams();

            if (teamsManager.getTeam(player) == teamsManager.getTeam(damager)) {
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

                    Pos shootPos = arrow.getTag(PLAYER_SHOOT_POS);

                    double distance = shootPos.distance(target.getPosition());
                    shooter.setLevel((int) distance); // Distance displayed in xp bar
                    if (distance > 50) { // If the distance is > 50 blocks then it's a longshot
                        shooter.setHealth(Math.min(shooter.getHealth() + 3, (float) shooter.getAttributeValue(Attribute.GENERIC_MAX_HEALTH)));

                        // Longshot sound & message
                        shooter.getInstance().getPlayers().forEach(players -> {
                            shooter.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_EYE_DEATH).pitch(1F).volume(2F).build(), shooter.getPosition());
                            players.playSound(Sound.sound().type(SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).pitch(1F).volume(1.5F).build(), players.getPosition());
                            players.sendMessage(MM.deserialize(
                                    "<gold>\uD83C\uDFF9 <b>LONGSHOT!</b></gold> <shooter> <yellow>a fait un longshot de <gold><b>"
                                            + Math.floor(distance * 10) / 10 + "</b> blocks</gold>!</yellow>",
                                    Placeholder.component("shooter", shooter.getName().color(TextColor.color(gameInstance.getTeams().getTeam(shooter).color())))
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

    private void registerDeathHandler() {
        combatMechanicsNode.addListener(PlayerDeathEvent.class, playerDeathEvent -> {
            GameInstance gameInstance = (GameInstance) playerDeathEvent.getPlayer().getInstance();
            Player deadPlayer = playerDeathEvent.getPlayer();

            deadPlayer.setGameMode(GameMode.SPECTATOR);
            deadPlayer.setCanPickupItem(false);
            deadPlayer.setInvisible(true); // Hide the deadPlayer

            deadPlayer.setRespawnPoint(deadPlayer.getPosition());

            new Entity(EntityType.LIGHTNING_BOLT).setInstance(gameInstance, deadPlayer.getPosition());

            Player killer = getLatestDamager(deadPlayer);
            if (killer == null) {
                gameInstance.sendMessage(MM.deserialize(
                        "<red>\uD83C\uDFF9</red> <deadplayer> <yellow>est mort!</yellow>",
                        Placeholder.component("deadplayer", deadPlayer.getName().color(TextColor.color(gameInstance.getTeams().getTeam(deadPlayer).color())))
                ));
            } else {
                gameInstance.sendMessage(MM.deserialize(
                        "<red>\uD83C\uDFF9</red> <deadplayer> <yellow>a été tué par</yellow> <killer><yellow>!</yellow>",
                        TagResolver.builder()
                                .resolver(Placeholder.component("deadplayer", deadPlayer.getName().color(TextColor.color(gameInstance.getTeams().getTeam(deadPlayer).color()))))
                                .resolver(Placeholder.component("killer", killer.getName().color(TextColor.color(gameInstance.getTeams().getTeam(killer).color()))))
                                .build()
                ));
            }
            gameInstance.getPlayers().forEach(player -> {
                if (gameInstance.getTeams().getTeam(player) == gameInstance.getTeams().getTeam(deadPlayer)) { // An ally dies
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

            // Check if all the players of his team are dead
            boolean allPlayersInTeamDead = gameInstance.getTeams().getPlayers(gameInstance.getTeams().getTeam(deadPlayer))
                    .stream()
                    .noneMatch(teamPlayer -> teamPlayer.getGameMode() != GameMode.SPECTATOR);

            if (allPlayersInTeamDead) {
                Team playerTeam = gameInstance.getTeams().getTeam(deadPlayer);
                Team opposingTeam = gameInstance.getTeams().getGameTeams().firstTeam() == playerTeam
                        ? gameInstance.getTeams().getGameTeams().secondTeam()
                        : gameInstance.getTeams().getGameTeams().firstTeam();

                gameInstance.finishGame(opposingTeam, playerTeam);
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

    public static EventNode<EntityInstanceEvent> create(GameInstance gameInstance) {
        return new CombatMechanics(gameInstance).combatMechanicsNode;
    }

    public static int getKills(Player player) {
        return player.hasTag(PLAYER_KILLS) ? player.getTag(PLAYER_KILLS) : 0;
    }

    public static Player getLatestDamager(Player player) {
        UUID lastDamagerUUID = player.getTag(LAST_DAMAGER_UUID);
        return lastDamagerUUID == null ? null : player.getInstance().getPlayerByUuid(lastDamagerUUID);
    }
}
