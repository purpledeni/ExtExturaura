package org.figuramc.figura.lua.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.nbt.CompoundTag;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.NbtToLua;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.backend2.Destination;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@LuaWhitelist
@LuaTypeDoc(
		name = "AvatarAPI",
		value = "avatar"
)
public class AvatarAPI {

	private final Avatar avatar;
	public final LuaTable storedStuff = new LuaTable();

	public AvatarAPI(Avatar avatar) {
		this.avatar = avatar;
	}

	private boolean bool(Permissions permissions) {
		return permissions.asBoolean(avatar.permissions.get(permissions));
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_nbt")
	public LuaTable getNBT() {
		return (LuaTable) NbtToLua.convert(avatar.nbt);
	}

	/* TODO: TYPE AUTO-DETECT AND MORE TYPES*/

	@LuaWhitelist
	@LuaMethodDoc("avatar.set_nbt")
	public void setNBT(@LuaNotNil String path, Object value, String valueType) {
		if(!avatar.isHost) throw new LuaError("Only the host avatar can be edited");
		String[] seperatedPath = path.replaceAll("\\\\.","THEDOT").split("\\.");
		for (int i = 1; i < seperatedPath.length; i++) {
			seperatedPath[i]=seperatedPath[i].replaceAll("THEDOT",".");
		}
		if(seperatedPath.length == 0) throw new LuaError("Invalid path");
		String vari = seperatedPath[seperatedPath.length-1];

		if(vari == null) throw new LuaError("Invalid path");
		CompoundTag currentNbt = avatar.nbt;
		if(seperatedPath.length > 1){

			for(var i=0; i < seperatedPath.length-1;i++){
				String part = seperatedPath[i];
				if(!currentNbt.contains(part)) throw new LuaError("Path " +path+" not found at "+part);
				currentNbt = currentNbt.getCompound(part);
			}
		}
		String type = (valueType == null ? (value == null || value == LuaValue.NIL ? "null" : value.getClass().toString()) : valueType).toLowerCase();
		int lastDot = type.lastIndexOf('.');
		if(lastDot != -1)
			type=type.substring(lastDot+1);
		switch(type){
			case "float":
				currentNbt.putFloat(vari,(float)value);
				break;
			case "int":
				currentNbt.putInt(vari,(int)value);
				break;
			case "short":
				currentNbt.putShort(vari,(short)value);
				break;
			case "double":
				currentNbt.putDouble(vari,(double)value);
				break;
			case "string":
				currentNbt.putString(vari,(String)value);
				break;
			case "bool":
			case "boolean":
				currentNbt.putBoolean(vari,(boolean)value);
				break;
			case "bytearray":
				currentNbt.putByteArray(vari,(byte[])value);
				break;
			case "null":
			case "nil":
				currentNbt.remove(vari);
				break;


			default:
				throw new LuaError("Value is invalid type "+type);
		}


		return;
	}


	@LuaWhitelist
	@LuaMethodDoc("avatar.forcePings")
	public AvatarAPI forcePings(boolean backend,boolean fsb) {
		avatar.forcePings = Destination.fromBool(backend,fsb);
		return this;
	}

	@LuaWhitelist
	@LuaMethodDoc(
			overloads = @LuaMethodOverload(
					argumentTypes = {String.class, Object.class},
					argumentNames = {"key", "value"}
			),
			value = "avatar.store"
	)
	public AvatarAPI store(@LuaNotNil String key, LuaValue value) {
		storedStuff.set(key, value == null ? LuaValue.NIL : value);
		return this;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_uuid")
	public String getUUID() {
		return avatar.owner.toString();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_current_instructions")
	public int getCurrentInstructions() {
		return avatar.luaRuntime == null ? 0 : avatar.luaRuntime.getInstructions();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_color")
	public String getColor() {
		return avatar.color;
	}

	@LuaWhitelist
	@LuaMethodDoc(
			overloads = {
					@LuaMethodOverload(
							argumentTypes = FiguraVec3.class,
							argumentNames = "color"
					),
					@LuaMethodOverload(
							argumentTypes = {Double.class, Double.class, Double.class},
							argumentNames = {"r", "g", "b"}
					),
					@LuaMethodOverload(
							argumentTypes = {FiguraVec3.class, String.class},
							argumentNames = {"color", "badgeName"}
					),
					@LuaMethodOverload(
							argumentTypes = {Double.class, Double.class, Double.class, String.class},
							argumentNames = {"r", "g", "b", "badgeName"}
					)
			},
			aliases = "color",
			value = "avatar.set_color"
	)
	public AvatarAPI setColor(Object r, Object g, Double b, String badge) {
		if ((g instanceof Number || g == null) && (badge == null || badge.isEmpty())){
			FiguraVec3 vec = LuaUtils.parseOneArgVec("setColor", r, (Number) g, b, 1d);
			avatar.color = ColorUtils.rgbToHex(vec);
		} else if (g instanceof String && r instanceof FiguraVec3) {
			FiguraVec3 vec = ((FiguraVec3) r).copy();
			avatar.badgeToColor.put((String) g, ColorUtils.rgbToHex(vec));
		} else {
			Number h;
			if (g instanceof Number)
				h = ((Number) g).doubleValue();
			else {
				h = 1d;
			}
			FiguraVec3 vec = LuaUtils.parseOneArgVec("setColor", r, h, b, 1d);
			avatar.badgeToColor.put(badge, ColorUtils.rgbToHex(vec));
		}
		return this;
	}

	@LuaWhitelist
	public AvatarAPI color(Object r, Object g, Double b, String badge) {
		return setColor(r, g, b, badge);
	}

	@LuaWhitelist
	@LuaMethodDoc(
			aliases = "badges",
			value = "avatar.get_badges"
	)
	public String getBadges() {
		Component component = Badges.fetchBadges(avatar.owner);
		return component.getString();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_version")
	public String getVersion() {
		return avatar.version.toString();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_authors")
	public String getAuthors() {
		return avatar.authors;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_name")
	public String getName() {
		return avatar.name;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_entity_name")
	public String getEntityName() {
		return avatar.entityName;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_size")
	public double getSize() {
		return avatar.getFileSize();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.has_texture")
	public boolean hasTexture() {
		return avatar.hasTexture;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.has_script_error")
	public boolean hasScriptError() {
		// useless I know
		return avatar.scriptError;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_permission_level")
	public String getPermissionLevel() {
		return avatar.permissions.getCategory().name();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_init_count")
	public int getInitCount() {
		return avatar.init.pre;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_entity_init_count")
	public int getEntityInitCount() {
		return avatar.init.post;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_init_count")
	public int getMaxInitCount() {
		return avatar.permissions.get(Permissions.INIT_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_tick_count")
	public int getTickCount() {
		return avatar.tick.getTotal();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_size_limit")
	public int getSizeLimit() {
		return NetworkStuff.getSizeLimit();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_tick_count")
	public int getMaxTickCount() {
		return avatar.permissions.get(Permissions.TICK_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_render_count")
	public int getRenderCount() {
		return avatar.render.getTotal();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_render_count")
	public int getMaxRenderCount() {
		return avatar.permissions.get(Permissions.RENDER_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_world_tick_count")
	public int getWorldTickCount() {
		return avatar.worldTick.getTotal();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_world_tick_count")
	public int getMaxWorldTickCount() {
		return avatar.permissions.get(Permissions.WORLD_TICK_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_world_render_count")
	public int getWorldRenderCount() {
		return avatar.worldRender.getTotal();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_world_render_count")
	public int getMaxWorldRenderCount() {
		return avatar.permissions.get(Permissions.WORLD_RENDER_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_complexity")
	public int getComplexity() {
		return avatar.complexity.pre;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_complexity")
	public int getMaxComplexity() {
		return avatar.permissions.get(Permissions.COMPLEXITY);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_remaining_particles")
	public int getRemainingParticles() {
		return avatar.particlesRemaining.peek();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_particles")
	public int getMaxParticles() {
		return avatar.permissions.get(Permissions.PARTICLES);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_remaining_sounds")
	public int getRemainingSounds() {
		return avatar.soundsRemaining.peek();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_sounds")
	public int getMaxSounds() {
		return avatar.permissions.get(Permissions.SOUNDS);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_volume")
	public int getVolume() {
		return avatar.permissions.get(Permissions.VOLUME);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_animation_complexity")
	public int getAnimationComplexity() {
		return avatar.animationComplexity;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_animation_complexity")
	public int getMaxAnimationComplexity() {
		return avatar.permissions.get(Permissions.BB_ANIMATIONS);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_animation_count")
	public int getAnimationCount() {
		return avatar.animation.pre;
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_animation_count")
	public int getMaxAnimationCount() {
		return avatar.permissions.get(Permissions.ANIMATION_INST);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_buffers_count")
	public int getBuffersCount() {
		return avatar.openBuffers.size();
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_buffers_count")
	public int getMaxBuffersCount() {
		return avatar.permissions.get(Permissions.BUFFERS_COUNT);
	}

	// @LuaWhitelist
	// @LuaMethodDoc("avatar.get_sockets_count")
	// public int getSocketsCount() {
	// 	return avatar.openSockets.size();
	// }

	// @LuaWhitelist
	// @LuaMethodDoc("avatar.get_max_sockets_count")
	// public int getMaxSocketsCount() {
	// 	return avatar.permissions.get(Permissions.MAX_SOCKETS);
	// }

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_texture_size")
	public int getMaxTextureSize() {
		return avatar.permissions.get(Permissions.TEXTURE_SIZE);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.get_max_buffer_size")
	public int getMaxBufferSize() {
		return avatar.permissions.get(Permissions.BUFFER_SIZE);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.can_edit_vanilla_model")
	public boolean canEditVanillaModel() {
		return bool(Permissions.VANILLA_MODEL_EDIT);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.can_edit_nameplate")
	public boolean canEditNameplate() {
		return bool(Permissions.NAMEPLATE_EDIT);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.can_render_offscreen")
	public boolean canRenderOffscreen() {
		return bool(Permissions.OFFSCREEN_RENDERING);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.can_use_custom_sounds")
	public boolean canUseCustomSounds() {
		return bool(Permissions.CUSTOM_SOUNDS);
	}

	@LuaWhitelist
	@LuaMethodDoc("avatar.can_have_custom_skull")
	public boolean canHaveCustomSkull() {
		return bool(Permissions.CUSTOM_SKULL);
	}

	@Override
	public String toString() {
		return "AvatarAPI";
	}
}
