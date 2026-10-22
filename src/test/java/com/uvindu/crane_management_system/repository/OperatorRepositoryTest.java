package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Operator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OperatorRepositoryTest {

    @Autowired
    private OperatorRepository operatorRepository;

    @Test
    void shouldSaveOperator() {
        Operator operator = createOperator("John Perera");

        Operator savedOperator = operatorRepository.save(operator);

        assertThat(savedOperator.getId()).isNotNull();
        assertThat(savedOperator.getOperatorName())
                .isEqualTo("John Perera");
    }

    @Test
    void shouldFindOperatorById() {
        Operator operator = operatorRepository.save(
                createOperator("Kasun Silva")
        );

        Optional<Operator> result =
                operatorRepository.findById(operator.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getOperatorName())
                .isEqualTo("Kasun Silva");
    }

    @Test
    void shouldReturnEmptyWhenOperatorIdDoesNotExist() {
        Optional<Operator> result =
                operatorRepository.findById(999999L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindAllOperators() {
        operatorRepository.save(createOperator("Operator One"));
        operatorRepository.save(createOperator("Operator Two"));
        operatorRepository.save(createOperator("Operator Three"));

        List<Operator> result = operatorRepository.findAll();

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(Operator::getOperatorName)
                .containsExactlyInAnyOrder(
                        "Operator One",
                        "Operator Two",
                        "Operator Three"
                );
    }

    @Test
    void shouldUpdateOperator() {
        Operator operator = operatorRepository.save(
                createOperator("Original Name")
        );

        operator.setOperatorName("Updated Name");

        Operator updatedOperator = operatorRepository.save(operator);

        assertThat(updatedOperator.getId())
                .isEqualTo(operator.getId());
        assertThat(updatedOperator.getOperatorName())
                .isEqualTo("Updated Name");
    }

    @Test
    void shouldDeleteOperator() {
        Operator operator = operatorRepository.save(
                createOperator("Operator To Delete")
        );

        Long operatorId = operator.getId();

        operatorRepository.deleteById(operatorId);

        Optional<Operator> result =
                operatorRepository.findById(operatorId);

        assertThat(result).isEmpty();
    }

    private Operator createOperator(String operatorName) {
        Operator operator = new Operator();
        operator.setOperatorName(operatorName);

        return operator;
    }
}