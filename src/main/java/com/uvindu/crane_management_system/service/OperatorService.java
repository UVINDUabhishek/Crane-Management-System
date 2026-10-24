package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.dto.CreateOperatorRequest;
import com.uvindu.crane_management_system.dto.OperatorResponse;
import com.uvindu.crane_management_system.model.Operator;
import com.uvindu.crane_management_system.repository.OperatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    // Create Operator
    public OperatorResponse createOperator(CreateOperatorRequest request) {

        Operator operator = new Operator();

        operator.setOperatorName(request.getOperatorName());

        Operator saveOperator = operatorRepository.save(operator);

        return mapToResponse(saveOperator);
    }

    // Get all Operators
    public List<OperatorResponse> getAllOperators() {

        return operatorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get Operator by ID
    public OperatorResponse getOperatorById(Long id) {

        Operator operator = operatorRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Operator not found"));

        return mapToResponse(operator);
    }

    // Delete Operator
    public void deleteOperator(Long id) {

        Operator operator = operatorRepository.findById(id)
                        .orElseThrow(()->
                                BusinessException.notFound("Operator not found"));

        operatorRepository.delete(operator);
    }

    // Mapping method
    public OperatorResponse mapToResponse(Operator operator) {

        OperatorResponse response = new OperatorResponse();

        response.setId(operator.getId());
        response.setOperatorName(operator.getOperatorName());

        return response;
    }

}
