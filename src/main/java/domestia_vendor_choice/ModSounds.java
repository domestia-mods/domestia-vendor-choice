package domestia_vendor_choice;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ModSounds {
	// Sound category.
	// PLAYERS makes these sounds behave like direct player feedback, not distant block noise.
	private static final SoundSource SOURCE_PLAYER_FEEDBACK = SoundSource.PLAYERS;

	// Vendor Machine: sales interface greeting.
	// Chime is closer to a small shop door bell than plain bell.
	private static final float VOLUME_MACHINE_SALES_DING = 1.80F;
	private static final float PITCH_MACHINE_SALES_DING_LOW = 1.25F;
	private static final float PITCH_MACHINE_SALES_DING_HIGH = 1.70F;

	// Vendor Machine: checkout success.
	// This is intentionally louder than GUI dings because it must read as a completed transaction.
	private static final float VOLUME_MACHINE_CHECKOUT_SUCCESS_ORB = 1.70F;
	private static final float VOLUME_MACHINE_CHECKOUT_SUCCESS_CHIME = 2.00F;
	private static final float VOLUME_MACHINE_CHECKOUT_SUCCESS_LEVEL = 0.85F;

	private static final float PITCH_MACHINE_CHECKOUT_SUCCESS_ORB = 1.60F;
	private static final float PITCH_MACHINE_CHECKOUT_SUCCESS_CHIME = 1.95F;
	private static final float PITCH_MACHINE_CHECKOUT_SUCCESS_LEVEL = 1.65F;

	// Vendor Machine: checkout / interaction error.
	private static final float VOLUME_MACHINE_ERROR = 1.80F;
	private static final float PITCH_MACHINE_ERROR = 0.45F;

	// Metallic container sounds.
	private static final float VOLUME_METAL_OPEN = 1.15F;
	private static final float VOLUME_METAL_CLOSE = 1.15F;

	private static final float PITCH_MACHINE_CONTROL_OPEN = 1.00F;
	private static final float PITCH_MACHINE_CONTROL_CLOSE = 1.00F;

	private static final float PITCH_SAFE_OPEN = 0.82F;
	private static final float PITCH_SAFE_CLOSE = 0.82F;

	private ModSounds() {
	}

	public static void playMachineSalesOpen(Player player) {
		playMachineSalesDing(player);
	}

	public static void playMachineSalesClose(Player player) {
		playMachineSalesDing(player);
	}

	public static void playMachineCheckoutSuccess(Player player) {
		if (player == null) {
			return;
		}

		playAtPlayer(
				player,
				SoundEvents.EXPERIENCE_ORB_PICKUP,
				VOLUME_MACHINE_CHECKOUT_SUCCESS_ORB,
				PITCH_MACHINE_CHECKOUT_SUCCESS_ORB
		);

		playAtPlayer(
				player,
				SoundEvents.NOTE_BLOCK_CHIME.value(),
				VOLUME_MACHINE_CHECKOUT_SUCCESS_CHIME,
				PITCH_MACHINE_CHECKOUT_SUCCESS_CHIME
		);

		playAtPlayer(
				player,
				SoundEvents.PLAYER_LEVELUP,
				VOLUME_MACHINE_CHECKOUT_SUCCESS_LEVEL,
				PITCH_MACHINE_CHECKOUT_SUCCESS_LEVEL
		);
	}

	public static void playMachineError(Player player) {
		if (player == null) {
			return;
		}

		playAtPlayer(
				player,
				SoundEvents.NOTE_BLOCK_BASS.value(),
				VOLUME_MACHINE_ERROR,
				PITCH_MACHINE_ERROR
		);
	}

	public static void playMachineControlOpen(Player player) {
		playMetalOpen(player, PITCH_MACHINE_CONTROL_OPEN);
	}

	public static void playMachineControlClose(Player player) {
		playMetalClose(player, PITCH_MACHINE_CONTROL_CLOSE);
	}

	public static void playSafeOpen(Player player) {
		playMetalOpen(player, PITCH_SAFE_OPEN);
	}

	public static void playSafeClose(Player player) {
		playMetalClose(player, PITCH_SAFE_CLOSE);
	}

	private static void playMachineSalesDing(Player player) {
		if (player == null) {
			return;
		}

		playAtPlayer(
				player,
				SoundEvents.NOTE_BLOCK_CHIME.value(),
				VOLUME_MACHINE_SALES_DING,
				PITCH_MACHINE_SALES_DING_LOW
		);

		playAtPlayer(
				player,
				SoundEvents.NOTE_BLOCK_CHIME.value(),
				VOLUME_MACHINE_SALES_DING,
				PITCH_MACHINE_SALES_DING_HIGH
		);
	}

	private static void playMetalOpen(Player player, float pitch) {
		if (player == null) {
			return;
		}

		playAtPlayer(
				player,
				SoundEvents.IRON_DOOR_OPEN,
				VOLUME_METAL_OPEN,
				pitch
		);
	}

	private static void playMetalClose(Player player, float pitch) {
		if (player == null) {
			return;
		}

		playAtPlayer(
				player,
				SoundEvents.IRON_DOOR_CLOSE,
				VOLUME_METAL_CLOSE,
				pitch
		);
	}

	private static void playAtPlayer(Player player, SoundEvent soundEvent, float volume, float pitch) {
		if (player == null || player.level().isClientSide()) {
			return;
		}

		player.level().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				soundEvent,
				SOURCE_PLAYER_FEEDBACK,
				volume,
				pitch
		);
	}
}