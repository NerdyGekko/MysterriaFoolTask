# Mysterria Trial Dev Task

This public repo is to hold my attempt at the Mysterria trial dev task.

## What am I making?

The task for the trial dev task is to make a simple plugin that adds the [Tampering / Grafting](https://lordofthemysteries.fandom.com/wiki/Fool_Pathway/Abilities#Sequence_1:_Attendant_of_Mysteries) ability from Sequence 1 Fool.
This readme will just outline the breakdown of the ability and how I want to implement it.

Grafting is quite complicated, so I'm going to break it down into a few different parts.

### Location Grafting
This is broken into two different parts. A single target teleport like effect.
That would let a player select a location in advance and then teleport either themselves or a selected target to that location.

The second part is an area grafting. Tying the 'end' of two points together.
This would create an area around the player and make it so that any entities that try and leave are just brought to the other side of the area.
I would like to try and get enderpearls to work with it so that it isn't too easy to  escape

#### Implementation
Location Grafting has been implemented as one item that has two different modes. First is single target grafting.
Shift+Left Click with the compass to set an anchor, you can then right-click with the compass to graft yourself back to
that area. Right-clicking a mob or other entity will graft that entity to the area instead.

Second, is area grafting. Toggle modes by shift+right-click with the compass in your hand. Right-clicking with area mode
will spawn a ring of particles around the caster. Attempting to leave one side of this area will teleport any entity to
the other side. This includes non-AI entities such as arrows, enderpearls, and snowballs (If their velocity is too high
it might be able to escape. Haven't been able to fix that yet.)

### Time Graft
This ability would graft the 'End' of a process to the 'Start'.
How this would actually translate to an in-game ability would be something like crops instantly growing from their first planted stage.
Baby mobs instantly growing up, or instantly finishing things like a brewing stand or furnace cooking.

Of course, this would also work in the opposite direction. Grafting the 'Start' of a process to the 'End'.
Grown mobs turn into babies, grown crops will turn back into their first stage.

#### Implementation
Time Graft has been implemented as a clock item. You can toggle between 'Forward' and 'Reverse' modes by shift+right-click
When on the forward mode Time Graft can connect the 'end' of a process to its 'beginning' which can do the following:
- Bring crops from their early stages to fully grown.
- Instantly grow baby NPCs into adults
- Instantly complete any action a block may be doing (Furnace Smelting. Potion Brewing, Etc)

When switched to the 'Reverse' mode it will do the opposite of all those things. Grafting the 'beginning' of a process
to its 'end'.

### Enchantment Graft
This is just a little thing I want to try and build. This would pull up a small GUI that would accept two items.
Grafting the enchantments from one item onto the other. This would apply the normal rules that minecraft has with adding
enchantments with a few small changes
- It would be able to add 'incompatable' enchantments together, Things like Silk Touch and Fortune, or Sharpness and Smite.

#### Implementation
Enchantment Graft has been implemented as an amethyst shard. When used it will bring up a GUI in which you can place, tools
weapons and books. Being able to graft the enchantments from one item onto another. This will use up the item with the enchants
in the process. It is able to do the following:
- Graft enchantments from a book, onto a plain or enchanted item
- Graft enchantments from an enchanted item, onto another plain or enchanted item
- Graft enchantments from an enchanted item onto a plain book, to make an enchanted book
- Bypass certain enchantment restrictions, such as no Silk Touch + Fortune, No Sharpness + Smite, Etc.

## AI Usage

I am using Anthropic's Opus 5.5 Model to help with some simple tasks. This will be a register of what I have used it for
- Basic Setup for a Paper 26.2 standalone Java Plugin
- Explinations for some ideas within plugin development
  - Listeners, command adding, raycasting
- Some help setting up the area detection and teleportation of entities for Location Grafting's Area mode/