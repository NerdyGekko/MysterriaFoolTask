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

### Time Graft
This ability would graft the 'End' of a process to the 'Start'.
How this would actually translate to an in-game ability would be something like crops instantly growing from their first planted stage.
Baby mobs instantly growing up, or instantly finishing things like a brewing stand or furnace cooking.

Of course, this would also work in the opposite direction. Grafting the 'Start' of a process to the 'End'.
Grown mobs turn into babies, grown crops will turn back into their first stage.

### Enchantment Graft
This is just a little thing I want to try and build. This would pull up a small GUI that would accept two items.
Grafting the enchantments from one item onto the other. This would apply the normal rules that minecraft has with adding
enchantments with a few small changes
- It would be able to add 'incompatable' enchantments together, Things like Silk Touch and Fortune, or Sharpness and Smite.
## AI Usage

I am using Anthropic's Opus 5.5 Model to help with some simple tasks. This will be a register of what I have used it for
- Basic Setup for a Paper 26.2 standalone Java Plugin