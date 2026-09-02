package com.elgourmat.careflow.adapter.out.persistence.mapper;

import com.elgourmat.careflow.adapter.out.persistence.entite.ClaimEntity;
import com.elgourmat.careflow.domain.Claim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClaimEntityMapper {

    @Mapping(target = "amount", source = "money.amount")
    @Mapping(target = "currency", source = "money.currency")
    ClaimEntity toEntity(Claim claim);

    @Mapping(target = "money", expression = "java(new com.elgourmat.careflow.domain.Money(entity.getAmount(), entity.getCurrency()))")
    Claim toDomain(ClaimEntity entity);
}
