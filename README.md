<h1 align="center">Domestia Vendor Choice</h1>

<p align="center">
  <img src="docs/assets/github-header.png" alt="Domestia Vendor Choice" width="100%">
</p>

<h3 align="center">
  Owner-protected trading tools.<br>
  <a href="https://github.com/domestia-mods/domestia-vendor-choice/releases">
    <img src="https://img.shields.io/badge/status-beta-yellowgreen" alt="Status">
  </a>
  <img src="https://img.shields.io/badge/Minecraft-26.1.2%2B-brightgreen" alt="Minecraft">
  <img src="https://img.shields.io/badge/Loader-Fabric-blue" alt="Fabric">
</h3>

<p align="center">
  <a href="https://ko-fi.com/Z5X220SWVK">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi">
  </a>
</p>

## New In-Game Blocks

### Vendor Machine

The Vendor Machine allows players to set up fully functional player-to-player trading based on direct item exchange.

#### Features

- The owner can list up to 5 different items for sale.
- The owner chooses the required payment item and price for each listed product.
- Items listed for sale are automatically displayed on the front panel of the Vendor Machine.
- The Vendor Machine automatically remembers its owner when placed.
- The owner can rename or label the Vendor Machine using an anvil.
- The owner name and custom label are displayed directly on the block.
- Payments from sales are deposited into a protected vault that only the owner can access.
- Protected Vendor Safe logistics is supported: stock can be restocked from an owner-matched Vendor Safe above the machine, and payments can be deposited into an owner-matched Vendor Safe below it.
- A successful purchase emits a short redstone transaction pulse from the utility faces: top, bottom, left, and right.<br>
- Only the owner can configure or modify the Vendor Machine settings.
- Other players cannot break the Vendor Machine.
- Operators can break another player’s Vendor Machine for administrative recovery, but they do not receive its contents.
- Only the owner can break their own Vendor Machine and recover the stored contents.

### Vendor Safe

The Vendor Safe gives players a secure place to store sales revenue and other valuable items.

#### Features

- Works as a 54-slot storage container.
- The Vendor Safe automatically remembers its owner when placed.
- The owner can rename or label the Vendor Safe using an anvil.
- The owner name and custom label are displayed directly on the block.
- Only the owner can access the contents of the Vendor Safe.
- Vendor Safes can participate in protected owner-matched Vendor Machine logistics.
- Other players cannot break the Vendor Safe.
- Operators can break another player’s Vendor Safe for administrative recovery, but they do not receive its contents.
- Only the owner can break their own Vendor Safe and recover the stored contents.

## Additional Features

- Dedicated interfaces with a familiar vanilla-style look and feel.
- PBR-oriented textures with normal and specular maps.
- Built-in sound feedback for block interactions.
- Protected owner-matched logistics between Vendor Machines and Vendor Safes.
- Vanilla hoppers are blocked from private Vendor Machine and Vendor Safe storage.
- Transaction pulse support for purchase-triggered redstone automation.

## Requirements

- Minecraft: 26.1.2+
- Fabric Loader
- Fabric API

## Status

Beta. This mod is currently tested on the Domestia server.

## Support

If you like this project, you can support development here:

<p align="center">
  <a href="https://ko-fi.com/Z5X220SWVK">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi">
  </a>
</p>

## License

See [LICENSE](LICENSE).
