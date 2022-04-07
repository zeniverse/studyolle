package com.studyolle.modules.account;

import com.querydsl.core.types.Predicate;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;

import java.util.Set;

public class AccountPredicates {

    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones){
        return QAccount.account.tags.any().in(tags).and(QAccount.account.zones.any().in(zones));
    }

}
