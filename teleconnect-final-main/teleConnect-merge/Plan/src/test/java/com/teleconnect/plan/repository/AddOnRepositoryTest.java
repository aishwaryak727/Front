package com.teleconnect.plan.repository;

import com.teleconnect.plan.entity.AddOn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddOnRepositoryTest {

    @Mock
    private AddOnRepository addOnRepository;

    @Test
    void findById_returnsAddOn() {
        AddOn addOn = new AddOn();
        addOn.setAddOnId(1);
        addOn.setName("SMS Pack");

        when(addOnRepository.findById(1)).thenReturn(Optional.of(addOn));

        Optional<AddOn> result = addOnRepository.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("SMS Pack");
    }
}
