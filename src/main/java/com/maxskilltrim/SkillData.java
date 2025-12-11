/*
 * Copyright (c) 2021, Hydrox6 <ikada@protonmail.ch>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.maxskilltrim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Skill;

@Getter
@AllArgsConstructor
enum SkillData {
    ATTACK(Skill.ATTACK),
    STRENGTH(Skill.STRENGTH),
    DEFENCE(Skill.DEFENCE),
    RANGED(Skill.RANGED),
    PRAYER(Skill.PRAYER),
    MAGIC(Skill.MAGIC),
    RUNECRAFT(Skill.RUNECRAFT),
    CONSTRUCTION(Skill.CONSTRUCTION),
    HITPOINTS(Skill.HITPOINTS),
    AGILITY(Skill.AGILITY),
    HERBLORE(Skill.HERBLORE),
    THIEVING(Skill.THIEVING),
    CRAFTING(Skill.CRAFTING),
    FLETCHING(Skill.FLETCHING),
    SLAYER(Skill.SLAYER),
    HUNTER(Skill.HUNTER),
    MINING(Skill.MINING),
    SMITHING(Skill.SMITHING),
    FISHING(Skill.FISHING),
    COOKING(Skill.COOKING),
    FIREMAKING(Skill.FIREMAKING),
    WOODCUTTING(Skill.WOODCUTTING),
    FARMING(Skill.FARMING),
    SAILING(Skill.SAILING);

    private final Skill skill;

    static SkillData get(int idx) {
        if (idx < 0 || idx >= values().length) {
            return null;
        }

        return values()[idx];
    }
}