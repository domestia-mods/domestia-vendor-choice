@echo off
setlocal
cd /d "%~dp0"

echo Removing legacy Vendor Stand files...
if exist "src\client\java\domestia_vendor_choice\client\VendorStandBlockEntityRenderState.java" del /f /q "src\client\java\domestia_vendor_choice\client\VendorStandBlockEntityRenderState.java"
if exist "src\client\java\domestia_vendor_choice\client\VendorStandBlockEntityRenderer.java" del /f /q "src\client\java\domestia_vendor_choice\client\VendorStandBlockEntityRenderer.java"
if exist "src\client\java\domestia_vendor_choice\client\VendorStandScreen.java" del /f /q "src\client\java\domestia_vendor_choice\client\VendorStandScreen.java"
if exist "src\main\java\domestia_vendor_choice\VendorStandBlock.java" del /f /q "src\main\java\domestia_vendor_choice\VendorStandBlock.java"
if exist "src\main\java\domestia_vendor_choice\VendorStandBlockEntity.java" del /f /q "src\main\java\domestia_vendor_choice\VendorStandBlockEntity.java"
if exist "src\main\java\domestia_vendor_choice\VendorStandData.java" del /f /q "src\main\java\domestia_vendor_choice\VendorStandData.java"
if exist "src\main\java\domestia_vendor_choice\VendorStandOpenPayload.java" del /f /q "src\main\java\domestia_vendor_choice\VendorStandOpenPayload.java"
if exist "src\main\java\domestia_vendor_choice\VendorStandSavePayload.java" del /f /q "src\main\java\domestia_vendor_choice\VendorStandSavePayload.java"
if exist "src\main\resources\assets\domestia_vendor_choice\blockstates\vendor_stand.json" del /f /q "src\main\resources\assets\domestia_vendor_choice\blockstates\vendor_stand.json"
if exist "src\main\resources\assets\domestia_vendor_choice\items\vendor_stand.json" del /f /q "src\main\resources\assets\domestia_vendor_choice\items\vendor_stand.json"
if exist "src\main\resources\assets\domestia_vendor_choice\models\block\vendor_note_stand.json" del /f /q "src\main\resources\assets\domestia_vendor_choice\models\block\vendor_note_stand.json"
if exist "src\main\resources\assets\domestia_vendor_choice\models\item\vendor_stand.json" del /f /q "src\main\resources\assets\domestia_vendor_choice\models\item\vendor_stand.json"
if exist "src\main\resources\data\domestia_vendor_choice\advancement\recipes\decorations\vendor_stand.json" del /f /q "src\main\resources\data\domestia_vendor_choice\advancement\recipes\decorations\vendor_stand.json"
if exist "src\main\resources\data\domestia_vendor_choice\loot_table\blocks\vendor_stand.json" del /f /q "src\main\resources\data\domestia_vendor_choice\loot_table\blocks\vendor_stand.json"
if exist "src\main\resources\data\domestia_vendor_choice\recipe\vendor_stand.json" del /f /q "src\main\resources\data\domestia_vendor_choice\recipe\vendor_stand.json"

echo Done. Vendor Note normalization cleanup complete.
endlocal
