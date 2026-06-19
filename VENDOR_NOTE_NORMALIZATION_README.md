# Vendor Note Normalization Patch

This patch renames the old Vendor Stand internal ID and files to Vendor Note.

Apply order:

1. Unpack this ZIP into the project root.
2. Run `vendor-note-normalization-cleanup.bat` on Windows, or `vendor-note-normalization-cleanup.sh` on Linux/macOS/Git Bash.
3. Build and test the project.

Deleted legacy files:

- `src/client/java/domestia_vendor_choice/client/VendorStandBlockEntityRenderState.java`
- `src/client/java/domestia_vendor_choice/client/VendorStandBlockEntityRenderer.java`
- `src/client/java/domestia_vendor_choice/client/VendorStandScreen.java`
- `src/main/java/domestia_vendor_choice/VendorStandBlock.java`
- `src/main/java/domestia_vendor_choice/VendorStandBlockEntity.java`
- `src/main/java/domestia_vendor_choice/VendorStandData.java`
- `src/main/java/domestia_vendor_choice/VendorStandOpenPayload.java`
- `src/main/java/domestia_vendor_choice/VendorStandSavePayload.java`
- `src/main/resources/assets/domestia_vendor_choice/blockstates/vendor_stand.json`
- `src/main/resources/assets/domestia_vendor_choice/items/vendor_stand.json`
- `src/main/resources/assets/domestia_vendor_choice/models/block/vendor_note_stand.json`
- `src/main/resources/assets/domestia_vendor_choice/models/item/vendor_stand.json`
- `src/main/resources/data/domestia_vendor_choice/advancement/recipes/decorations/vendor_stand.json`
- `src/main/resources/data/domestia_vendor_choice/loot_table/blocks/vendor_stand.json`
- `src/main/resources/data/domestia_vendor_choice/recipe/vendor_stand.json`
