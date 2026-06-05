# Changelog

## Unreleased

## 1.1.4-beta

### Added

- Vendor Machine now supports hopper IO for automated stock restock and vault collection.
- Hoppers can restock configured stock slots from the top side and the viewer-right side of the machine.
- Hoppers can drain received payments from the vault through the bottom side.
- Vendor Machine now emits a short redstone transaction pulse after a successful purchase.
- Transaction pulse is emitted from the utility faces of the Vendor Machine: top, bottom, left, and right relative to the machine facing.
- Front and back faces do not emit transaction pulse signals because they are reserved for the sales and control interfaces.
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
