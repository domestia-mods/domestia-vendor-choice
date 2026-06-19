# Changelog

## Released

## 1.4.1

### Added

- Added Vendor Scrap, a new ingredient recovered as a rare bonus from iron, copper, and gold ores.

### Changed

- Updated Vendor Note, Vendor Hopper, Vendor Safe, and Vendor Machine recipes around Vendor Scrap progression.
- All Domestia Vendor Choice crafting recipes now unlock from Vendor Scrap.
- Organized language keys by feature area and moved remaining user-facing fallback labels into the language file.

### Fixed

- Added missing localized break-denied messages for Vendor Machine and Vendor Safe.

## 1.3.1

### Added

- Added Vendor Note, an owner-authored public note block with floor and wall variants.
- Vendor Note supports owner-only editing, public read-only viewing, and persistent owner, title, and body data.
- Added full text-selection support to the Vendor Note editor, including keyboard selection, mouse selection, copy, cut, paste, select-all, and selection replacement while typing.

### Security

- Vendor Machine Sales GUI no longer synchronizes raw Vault item stacks to buyers. The sales screen now receives server-computed purchase limits instead of private Vault contents, while checkout still validates the real Vault on the server.

### Changed

- Normalized Vendor Note registry, resource, data component, networking, and internal class names around `vendor_note`.
- Vendor Note now uses a custom textured text interface for editing and reading notes.
- Vendor Note body wrapping now prefers whitespace boundaries instead of splitting words across lines.
- Improved Vendor Note model orientation, item rendering, text rendering, and interaction shapes.
- User-facing naming has been standardized on Vendor Note; internal floor-model naming remains implementation-only.
- Localized remaining Vendor Note and Vendor Hopper UI text constants through the language file.
- Unified shared owner/admin access checks through a common VendorAccess helper.
- Unified shared player inventory and hotbar menu-slot layout through a common VendorMenuSlots helper.

### Fixed

- Fixed Vendor Note interaction issues on the tilted floor surface.
- Fixed Vendor Machine label text visibility in low light.
- Fixed Vendor Machine front display item orientation for asymmetric item models.

## 1.2.3

### Added

- Added downward template diversion for Vendor Hopper output, enabling simple sorting-bus layouts with a Vendor Hopper placed directly below another Vendor Hopper.

### Changed

- Improved Vendor Hopper transfer throughput so hopper chains move items more consistently with vanilla hopper pacing.

### Fixed

- Fixed unreliable Vendor Hopper dropped-item pickup for item entities moving through or near the hopper body.
- Fixed a minor Vendor Machine recipe issue.

## 1.2.2

### Added

- Added Vendor Hopper, an owner-protected logistics block with Template slots for filtering and Buffer slots for item transfer.
- Vendor Hopper can interact with Vendor Machine, Vendor Safe, and other Vendor Hoppers when ownership matches.
- Vendor Hopper can also work with regular vanilla containers and collect dropped items using vanilla-like pickup behavior.
- Vendor Hopper can receive input from hopper minecarts and vanilla hoppers while still preventing extraction by them.

### Changed

- Vendor Machine, Vendor Safe, and Vendor Hopper logistics now use protected owner-aware rules when interacting with Vendor blocks.
- Vendor Hopper input now routes items through Template and Buffer rules before accepting them.
- Vendor Hopper output now uses Buffer slots only; Template items are never transferred by logistics.
- Vendor Safe, Vendor Hopper, and Vendor Machine recipes have been updated for protected logistics progression.

### Fixed

- Vendor Hopper no longer blocks chest interaction when placed above a chest.
- Vendor Hopper item now renders as a normal flat inventory item instead of a large held block model.
- Vendor Hopper cannot be extracted from by vanilla hoppers or hopper minecarts.
- Vendor Hopper no longer outputs into vanilla hoppers.
- Vendor Hopper interaction with Vendor Machine now respects stock, price, and vault boundaries.
- Vendor Hopper template items are no longer exposed to automated transfer.

## 1.1.4-beta

### Added

- Vendor Machine now supports hopper IO for automated stock restock and vault collection.
- Hoppers can restock configured stock slots from the top or viewer-right side and drain received payments from the vault through the bottom side.
- Vendor Machine now emits a short redstone transaction pulse from its utility faces after a successful purchase.
- Transaction pulse can activate adjacent redstone components and command blocks, including a command block placed directly under the Vendor Machine.

### Fixed

- Sales and control interfaces now live-refresh when hopper restock or vault drain changes the Vendor Machine inventory.
- Sold-out stock positions can now be restocked by hoppers during an active sales session without allowing hoppers to fill unrelated empty stock slots.
- Sales interface now live-refreshes stock display after external stock changes, including command block restock triggered by the transaction pulse.

## 1.1.3-beta

### Added

- Vendor Machine block.
- Vendor Safe block.
- Owner-only control access.
- Owner-only safe access.
- Public sales interface.
- Stock, price, payment, and vault logic.
- Front panel stock item rendering.
- Owner/custom label rendering.
- Vanilla sound effects for machine and safe interactions.
- PBR-ready block textures with normal and specular maps.

### Changed

- Vendor Machine no longer emits block light.
- Front stock item rendering uses full-bright lighting.

### Fixed

- Removed stock items now disappear from the Vendor Machine front panel.
- Stock item tooltip is shown in the sales interface.
