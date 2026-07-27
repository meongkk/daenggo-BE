package com.daenggo.backend.group.dto;

import com.daenggo.backend.group.entity.Group;
import com.daenggo.backend.group.entity.GroupMember;
import com.daenggo.backend.group.entity.GroupMemberRole;
import com.daenggo.backend.pet.entity.Pet;
import com.daenggo.backend.walk.entity.WalkRecord;
import com.daenggo.backend.walk.entity.WalkRecordPet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GroupResponseDto {

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Summary {

        private final Long groupId;
        private final String name;
        private final String description;
        private final long memberCount;
        private final GroupMemberRole myRole;

        public static Summary from(
                final Group group,
                final long memberCount,
                final GroupMemberRole myRole
        ) {
            return new Summary(
                    group.getId(),
                    group.getName(),
                    group.getDescription(),
                    memberCount,
                    myRole
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Detail {

        private final Long groupId;
        private final String name;
        private final String description;
        private final Long ownerId;
        private final String ownerNickname;
        private final long memberCount;
        private final GroupMemberRole myRole;
        private final LocalDateTime createdAt;

        public static Detail from(
                final Group group,
                final long memberCount,
                final GroupMemberRole myRole
        ) {
            return new Detail(
                    group.getId(),
                    group.getName(),
                    group.getDescription(),
                    group.getOwner().getId(),
                    group.getOwner().getNickname(),
                    memberCount,
                    myRole,
                    group.getCreatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Member {

        private final Long memberId;
        private final Long userId;
        private final String nickname;
        private final String profileImageUrl;
        private final GroupMemberRole role;
        private final LocalDateTime joinedAt;

        public static Member from(final GroupMember member) {
            return new Member(
                    member.getId(),
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getUser().getImage(),
                    member.getRole(),
                    member.getJoinedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GroupPet {

        private final Long petId;
        private final String name;
        private final String profileImageUrl;
        private final boolean primary;
        private final Long ownerUserId;
        private final String ownerNickname;

        public static GroupPet from(final Pet pet) {
            return new GroupPet(
                    pet.getId(),
                    pet.getName(),
                    pet.getImage(),
                    pet.isPrimary(),
                    pet.getUser().getId(),
                    pet.getUser().getNickname()
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GroupWalk {

        private final Long walkRecordId;
        private final String title;
        private final String memo;
        private final LocalDateTime startedAt;
        private final LocalDateTime endedAt;
        private final BigDecimal distanceM;
        private final Integer durationSec;
        private final Integer avgPaceSec;
        private final Long ownerUserId;
        private final String ownerNickname;
        private final List<GroupWalkPet> pets;

        public static GroupWalk from(
                final WalkRecord walkRecord,
                final List<WalkRecordPet> walkRecordPets
        ) {
            return new GroupWalk(
                    walkRecord.getWalkRecordId(),
                    walkRecord.getTitle(),
                    walkRecord.getMemo(),
                    walkRecord.getStartedAt(),
                    walkRecord.getEndedAt(),
                    walkRecord.getDistanceM(),
                    walkRecord.getDurationSec(),
                    walkRecord.getAvgPaceSec(),
                    walkRecord.getUser().getId(),
                    walkRecord.getUser().getNickname(),
                    walkRecordPets.stream()
                            .map(GroupWalkPet::from)
                            .toList()
            );
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GroupWalkPet {

        private final Long petId;
        private final String name;
        private final String profileImageUrl;

        public static GroupWalkPet from(final WalkRecordPet walkRecordPet) {
            final Pet pet = walkRecordPet.getPet();
            return new GroupWalkPet(
                    pet.getId(),
                    pet.getName(),
                    pet.getImage()
            );
        }
    }
}
