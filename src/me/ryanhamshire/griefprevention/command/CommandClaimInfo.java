/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.command;

import com.flowpowered.math.vector.Vector3d;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimWorldManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;

public class CommandClaimInfo implements CommandExecutor {

    private static final Text NONE = Text.of(TextColors.GRAY, "none");

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String claimIdentifier = ctx.<String>getOne("id").orElse(null);
        Player player = null;
        if (src instanceof Player) {
            player = (Player) src;
        }

        if (player == null && claimIdentifier == null) {
            src.sendMessage(Text.of("No valid player or claim id found."));
            return CommandResult.success();
        }

        Claim claim = null;
        if (player != null && claimIdentifier == null) {
            claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        } else {
            for (WorldProperties worldProperties : Sponge.getServer().getAllWorldProperties()) {
                ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(worldProperties);
                for (Claim worldClaim : claimWorldManager.getWorldClaims()) {
                    if (worldClaim.id.toString().equalsIgnoreCase(claimIdentifier)) {
                        claim = worldClaim;
                        break;
                    }
                    Text claimName = worldClaim.getClaimData().getClaimName();
                    if (claimName != null && !claimName.isEmpty()) {
                        if (claimName.toPlain().equalsIgnoreCase(claimIdentifier)) {
                            claim = worldClaim;
                            break;
                        }
                    }
                }
                if (claim != null) {
                    break;
                }
            }
        }

        if (claim == null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
            return CommandResult.success();
        }

        UUID ownerUniqueId = claim.getClaimData().getOwnerUniqueId();
        if (claim.parent != null) {
            ownerUniqueId = claim.parent.ownerID;
        }
        // if not owner of claim, validate perms
        if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            if (!claim.getClaimData().getContainers().contains(player.getUniqueId()) 
                    && !claim.getClaimData().getBuilders().contains(player.getUniqueId())
                    && !claim.getClaimData().getManagers().contains(player.getUniqueId())
                    && !player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_OTHERS)) {
                player.sendMessage(Text.of(TextColors.RED, "You do not have permission to view information in this claim.")); 
                return CommandResult.success();
            }
        }

        User owner = null;
        if (!claim.isWildernessClaim()) {
            owner =  GriefPrevention.getOrCreateUser(ownerUniqueId);
        }

        Text name = claim.getClaimData().getClaimName();
        Text greeting = claim.getClaimData().getGreetingMessage();
        Text farewell = claim.getClaimData().getFarewellMessage();
        String accessors = "";
        String builders = "";
        String containers = "";
        String managers = "";
        
        Location<World> southWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.lesserBoundaryCorner.getPosition().getX(), 65.0D, claim.greaterBoundaryCorner.getPosition().getZ()));
        Location<World> northWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.lesserBoundaryCorner.getPosition().getX(), 65.0D, claim.lesserBoundaryCorner.getPosition().getZ()));
        Location<World> southEast = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.greaterBoundaryCorner.getPosition().getX(), 65.0D, claim.greaterBoundaryCorner.getPosition().getZ()));
        Location<World> northEast = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.greaterBoundaryCorner.getPosition().getX(), 65.0D, claim.lesserBoundaryCorner.getPosition().getZ()));
        // String southWestCorner = 
        Date created = null;
        Date lastActive = null;
        try {
            Instant instant = Instant.parse(claim.getClaimData().getDateCreated());
            created = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        try {
            Instant instant = Instant.parse(claim.getClaimData().getDateLastActive());
            lastActive = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        Text claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, name == null ? NONE : name);
        for (UUID uuid : claim.getClaimData().getAccessors()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            accessors += user.getName() + " ";
        }
        for (UUID uuid : claim.getClaimData().getBuilders()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            builders += user.getName() + " ";
        }
        for (UUID uuid : claim.getClaimData().getContainers()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            containers += user.getName() + " ";
        }
        for (UUID uuid : claim.getClaimData().getManagers()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            managers += user.getName() + " ";
        }

        TextColor claimTypeColor = TextColors.GREEN;
        if (claim.isAdminClaim()) {
            if (claim.isSubdivision()) {
                claimTypeColor = TextColors.DARK_AQUA;
            } else {
                claimTypeColor = TextColors.RED;
            }
        } else if (claim.isSubdivision()) {
            claimTypeColor = TextColors.AQUA;
        }
        Text claimId = Text.join(Text.of(TextColors.YELLOW, "UUID", TextColors.WHITE, " : ",
                Text.builder()
                        .append(Text.of(TextColors.GRAY, claim.id.toString()))
                        .onShiftClick(TextActions.insertText(claim.id.toString())).build()));
        Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GOLD, owner != null ? owner.getName() : "administrator");
        Text claimType = Text.of(TextColors.YELLOW, "Type", TextColors.WHITE, " : ", 
                TextColors.GREEN, claim.cuboid ? "3D " : "2D ", claimTypeColor, claim.type.name(),
                TextColors.WHITE, " (", TextColors.GRAY, claim.getArea(), " blocks",
                TextColors.WHITE, " )");
        Text claimInherit = Text.of(TextColors.YELLOW, "InheritParent", TextColors.WHITE, " : ", claim.inheritParent ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF"));
        Text claimFarewell = Text.of(TextColors.YELLOW, "Farewell", TextColors.WHITE, " : ", TextColors.RESET,
                farewell == null ? NONE : farewell);
        Text claimGreeting = Text.of(TextColors.YELLOW, "Greeting", TextColors.WHITE, " : ", TextColors.RESET,
                greeting == null ? NONE : greeting);
        Text pvp = Text.of(TextColors.YELLOW, "PvP", TextColors.WHITE, " : ", TextColors.RESET, claim.isPvpEnabled() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF"));
        Text southWestCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "SW", TextColors.WHITE, " : ", TextColors.GRAY, southWest.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, southWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to SW corner of claim.")))
                .build();
        Text southEastCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "SE", TextColors.WHITE, " : ", TextColors.GRAY, southEast.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, southEast, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to SE corner of claim.")))
                .build();
        Text southCorners = Text.builder()
                .append(Text.of(TextColors.YELLOW, "SouthCorners", TextColors.WHITE, " : "))
                .append(southWestCorner)
                .append(southEastCorner).build();
        Text northWestCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "NW", TextColors.WHITE, " : ", TextColors.GRAY, northWest.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, northWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to NW corner of claim.")))
                .build();
        Text northEastCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "NE", TextColors.WHITE, " : ", TextColors.GRAY, northEast.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, northEast, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to NE corner of claim.")))
                .build();
        Text northCorners = Text.builder()
                .append(Text.of(TextColors.YELLOW, "NorthCorners", TextColors.WHITE, " : "))
                .append(northWestCorner)
                .append(northEastCorner).build();
        Text claimAccessors = Text.of(TextColors.YELLOW, "Accessors", TextColors.WHITE, " : ", TextColors.BLUE, accessors.equals("") ? NONE : accessors);
        Text claimBuilders = Text.of(TextColors.YELLOW, "Builders", TextColors.WHITE, " : ", TextColors.YELLOW, builders.equals("") ? NONE : builders);
        Text claimContainers = Text.of(TextColors.YELLOW, "Containers", TextColors.WHITE, " : ", TextColors.GREEN, containers.equals("") ? NONE : containers);
        Text claimCoowners = Text.of(TextColors.YELLOW, "Managers", TextColors.WHITE, " : ", TextColors.GOLD, managers.equals("") ? NONE : managers);
        Text dateCreated = Text.of(TextColors.YELLOW, "Created", TextColors.WHITE, " : ", TextColors.GRAY, created != null ? created : "Unknown");
        Text dateLastActive = Text.of(TextColors.YELLOW, "LastActive", TextColors.WHITE, " : ", TextColors.GRAY, lastActive != null ? lastActive : "Unknown");
        Text worldName = Text.of(TextColors.YELLOW, "World", TextColors.WHITE, " : ", TextColors.GRAY, claim.world.getProperties().getWorldName());
        Text footer = Text.of(TextColors.WHITE, TextStyles.STRIKETHROUGH, "------------------------------------------");
        if (claim.parent != null) {
            GriefPrevention.sendMessage(src,
                    Text.of(footer, "\n",
                            claimName, "\n",
                            ownerLine, "\n",
                            claimType, "\n",
                            claimInherit, "\n",
                            claimAccessors, "\n",
                            claimBuilders, "\n",
                            claimContainers, "\n",
                            claimCoowners, "\n",
                            claimGreeting, "\n",
                            claimFarewell, "\n",
                            pvp, "\n",
                            worldName, "\n",
                            dateCreated, "\n",
                            dateLastActive, "\n",
                            claimId, "\n",
                            northCorners, "\n",
                            southCorners, "\n",
                            footer));
        } else if (!claim.isWildernessClaim()) {
            GriefPrevention.sendMessage(src,
                    Text.of(footer, "\n",
                            claimName, "\n",
                            ownerLine, "\n",
                            claimType, "\n",
                            claimAccessors, "\n",
                            claimBuilders, "\n",
                            claimContainers, "\n",
                            claimCoowners, "\n",
                            claimGreeting, "\n",
                            claimFarewell, "\n",
                            pvp, "\n",
                            worldName, "\n",
                            dateCreated, "\n",
                            dateLastActive, "\n",
                            claimId, "\n",
                            northCorners, "\n",
                            southCorners, "\n",
                            footer));
        } else { // wilderness
            GriefPrevention.sendMessage(src,
                    Text.of(footer, "\n",
                            claimName, "\n",
                            ownerLine, "\n",
                            claimType, "\n",
                            claimGreeting, "\n",
                            claimFarewell, "\n",
                            pvp, "\n",
                            worldName, "\n",
                            dateCreated, "\n",
                            claimId, "\n",
                            footer));
        }

        return CommandResult.success();
    }
}
