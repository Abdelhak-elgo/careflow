package com.elgourmat.careflow.adapter.in.rest.mapper;

import com.elgourmat.careflow.adapter.in.rest.dto.AdminDecisionRequest;
import com.elgourmat.careflow.adapter.in.rest.dto.ClaimResponse;
import com.elgourmat.careflow.adapter.in.rest.dto.SubmitClaimRequest;
import com.elgourmat.careflow.application.port.in.DecideClaimUseCase.DecideClaimCommand;
import com.elgourmat.careflow.application.port.in.SubmitClaimUseCase.SubmitClaimCommand;
import com.elgourmat.careflow.domain.Claim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClaimRestMapper {

    SubmitClaimCommand toCommand(SubmitClaimRequest request);

    @Mapping(target = "amount", source = "money.amount")
    @Mapping(target = "currency", source = "money.currency")
    ClaimResponse toResponse(Claim claim);

    List<ClaimResponse> toResponses(List<Claim> claims);

    default DecideClaimCommand toDecideCommand(UUID claimId, AdminDecisionRequest request) {
        return new DecideClaimCommand(claimId, request.decision(), request.reason());
    }
}
