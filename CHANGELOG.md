# Changelog

## Unreleased

## 1.1.4-beta

### Added

- Vendor Machine now emits a short redstone transaction pulse after a successful purchase.
- Transaction pulse is emitted from the utility faces of the Vendor Machine: top, bottom, left, and right relative to the machine facing. Transaction pulse can activate adjacent redstone components and command blocks, including a command block placed directly under the Vendor Machine.
- Vendor Machine now supports protected owner-matched logistics with Vendor Safes: Vendor Safe placed above a Vendor Machine can automatically restock configured stock positions when both blocks have the same owner; Vendor Safe placed below a Vendor Machine can automatically receive payments from the machine vault when both blocks have the same owner.
- Vendor Safe capacity increased to 54 slots.

### Changed

- Vanilla hopper access to Vendor Machine storage is now fully disabled.
- Vanilla hopper access to Vendor Safe storage is now fully disabled.
- Automated private logistics now use owner-matched Vendor Machine and Vendor Safe transfers instead of vanilla hopper IO.
- Vendor Machine secure logistics now runs server-side while the block is loaded, without requiring any GUI to be open.

### Fixed

- Fixed a privacy issue where vanilla hoppers could drain Vendor Machine vault contents.
- Fixed a privacy issue where vanilla hoppers could insert into or drain Vendor Safe contents.
- Sales and control interfaces now live-refresh when protected Vendor Machine / Vendor Safe transfers change the machine inventory.
- Sold-out stock positions can be restocked from an owner-matched Vendor Safe during an active sales session without allowing unrelated empty stock slots to be filled.
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
