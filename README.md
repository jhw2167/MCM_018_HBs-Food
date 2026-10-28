# HB's Food With Benefits

Simple food overhaul that grants some diverse benefits to eating a variety of foods.

## Concept

- Foods are sorted into groups based on the config such as "Meats", "Vegetables", "Fruits".
- Each group gives some potion effect when eaten ( I know a lot of mods do this, but hear me out )
  - Meats: Strength
  - Vegetables: Haste
  - Grains: Swiftness
  
- Bonus Potion effects are only granted for foods that return at least 5 hunger points (2.5 bars), configurable.
- The bonus potion effect is rewarded for 10 seconds per hunger point restored, configurable.
- There are other bonus categories to give more diversity to food:
  - Lingering: The effect lasts double
  - Robust: The effect is amplified by a tier
  - Hearty: Bonus Health restoration 

- If you eat a food and it saturates you to at least 5 points, it will also reward you with the bonus
from the previous food you ate. Allowing you to stack bonuses.

- There is one more bonus, if you possess all group bonuses (Strength, haste, swiftness) you 
receive the additional "Healthy" bonus effect, by default it is resistance 

## Changes to Vanilla Systems

- Rapid healing from food when hurt is disabled. More of your healing will come from Healing Potions or sleeping
- Hunger drains faster while you are hurt
- Beds now restore health after a good nights sleep
- Adds a new health potion recipe for early game access
