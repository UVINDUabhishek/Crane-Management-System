package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateOperatorRequest;
import com.uvindu.crane_management_system.dto.OperatorResponse;
import com.uvindu.crane_management_system.model.Operator;
import com.uvindu.crane_management_system.repository.OperatorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperatorServiceTest extends BaseUnitTest {

    @Mock
    private OperatorRepository operatorRepository;

    @InjectMocks
    private OperatorService operatorService;


    @Test
    void shouldCreateOperatorSuccessfully() {
        // Arrange
        CreateOperatorRequest request = new CreateOperatorRequest();
        request.setOperatorName("John Operator");

        Operator savedOperator = new Operator();
        savedOperator.setId(1L);
        savedOperator.setOperatorName("John Operator");

        when(operatorRepository.save(any(Operator.class)))
                .thenReturn(savedOperator);

        // Act
        OperatorResponse result =
                operatorService.createOperator(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOperatorName())
                .isEqualTo("John Operator");

        verify(operatorRepository).save(any(Operator.class));
    }


    @Test
    void shouldGetAllOperators() {
        // Arrange
        Operator operator1 = new Operator();
        operator1.setId(1L);
        operator1.setOperatorName("John Operator");

        Operator operator2 = new Operator();
        operator2.setId(2L);
        operator2.setOperatorName("David Operator");

        when(operatorRepository.findAll())
                .thenReturn(List.of(operator1, operator2));

        // Act
        List<OperatorResponse> result =
                operatorService.getAllOperators();

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getOperatorName())
                .isEqualTo("John Operator");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getOperatorName())
                .isEqualTo("David Operator");

        verify(operatorRepository).findAll();
    }


    @Test
    void shouldGetOperatorByIdSuccessfully() {
        // Arrange
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setOperatorName("John Operator");

        when(operatorRepository.findById(1L))
                .thenReturn(Optional.of(operator));

        // Act
        OperatorResponse result =
                operatorService.getOperatorById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOperatorName())
                .isEqualTo("John Operator");

        verify(operatorRepository).findById(1L);
    }


    @Test
    void shouldThrowExceptionWhenOperatorDoesNotExist() {
        // Arrange
        when(operatorRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                operatorService.getOperatorById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Operator not found");

        verify(operatorRepository).findById(99L);
    }


    @Test
    void shouldDeleteOperatorSuccessfully() {
        // Arrange
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setOperatorName("John Operator");

        when(operatorRepository.findById(1L))
                .thenReturn(Optional.of(operator));

        // Act
        operatorService.deleteOperator(1L);

        // Assert
        verify(operatorRepository).findById(1L);
        verify(operatorRepository).delete(operator);
    }


    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOperator() {
        // Arrange
        when(operatorRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                operatorService.deleteOperator(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Operator not found");

        verify(operatorRepository).findById(99L);
        verify(operatorRepository, never()).delete(any());
    }


    @Test
    void shouldMapOperatorToResponse() {
        // Arrange
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setOperatorName("John Operator");

        // Act
        OperatorResponse result =
                operatorService.mapToResponse(operator);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOperatorName())
                .isEqualTo("John Operator");
    }
}