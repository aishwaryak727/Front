package com.teleconnect.plan.service;

import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.entity.AddOn;
import com.teleconnect.plan.repository.AddOnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddOnServiceTest {

    @Mock
    private AddOnRepository repository;

    @InjectMocks
    private AddOnService service;

    private AddOn mockAddOn;
    private AddOnRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockAddOn = new AddOn();
        mockAddOn.setAddOnId(1);
        mockAddOn.setName("1GB Night Pack");
        mockAddOn.setType(AddOn.AddOnType.DataTopup);
        mockAddOn.setQuota(new BigDecimal("1024.00"));
        mockAddOn.setValidityDays(7);
        mockAddOn.setPrice(new BigDecimal("29.00"));
        mockAddOn.setStatus(AddOn.AddOnStatus.A);

        mockRequest = new AddOnRequest();
        mockRequest.setName("1GB Night Pack");
        mockRequest.setType("DataTopup");
        mockRequest.setQuota(new BigDecimal("1024.00"));
        mockRequest.setValidityDays(7);
        mockRequest.setPrice(new BigDecimal("29.00"));
    }

    // ── CREATE ADD-ON TESTS ───────────────────────────────────────────────────

    @Test
    void createAddOn_withValidRequest_shouldSaveSuccessfully() {
        when(repository.save(any(AddOn.class))).thenReturn(mockAddOn);
        service.createAddOn(mockRequest);
        verify(repository, times(1)).save(any(AddOn.class));
    }

    @Test
    void createAddOn_shouldSetStatusToActiveByDefault() {
        when(repository.save(any(AddOn.class))).thenReturn(mockAddOn);
        service.createAddOn(mockRequest);
        verify(repository).save(argThat(addOn ->
            addOn.getStatus() == AddOn.AddOnStatus.A
        ));
    }

    @Test
    void createAddOn_withInvalidType_shouldThrowException() {
        mockRequest.setType("InvalidType");
        assertThrows(IllegalArgumentException.class, () -> {
            service.createAddOn(mockRequest);
        });
    }

    @Test
    void createAddOn_withISDPackType_shouldSaveSuccessfully() {
        mockRequest.setType("ISDPack");
        when(repository.save(any(AddOn.class))).thenReturn(mockAddOn);
        service.createAddOn(mockRequest);
        verify(repository, times(1)).save(any(AddOn.class));
    }

    @Test
    void createAddOn_withRoamingPackType_shouldSaveSuccessfully() {
        mockRequest.setType("RoamingPack");
        when(repository.save(any(AddOn.class))).thenReturn(mockAddOn);
        service.createAddOn(mockRequest);
        verify(repository, times(1)).save(any(AddOn.class));
    }

    @Test
    void createAddOn_withSMSPackType_shouldSaveSuccessfully() {
        mockRequest.setType("SMSPack");
        when(repository.save(any(AddOn.class))).thenReturn(mockAddOn);
        service.createAddOn(mockRequest);
        verify(repository, times(1)).save(any(AddOn.class));
    }

    // ── GET ALL ADD-ONS TESTS ─────────────────────────────────────────────────

    @Test
    void getAllAddOns_whenAddOnsExist_shouldReturnList() {
        when(repository.findAll()).thenReturn(Arrays.asList(mockAddOn));
        List<AddOnResponse> result = service.getAllAddOns();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1GB Night Pack", result.get(0).getName());
    }

    @Test
    void getAllAddOns_whenNoAddOns_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        List<AddOnResponse> result = service.getAllAddOns();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAddOns_shouldReturnAllFieldsCorrectly() {
        when(repository.findAll()).thenReturn(Arrays.asList(mockAddOn));
        List<AddOnResponse> result = service.getAllAddOns();
        AddOnResponse res = result.get(0);
        assertEquals(1, res.getAddOnId());
        assertEquals("1GB Night Pack", res.getName());
        assertEquals("DataTopup", res.getType());
        assertEquals(new BigDecimal("1024.00"), res.getQuota());
        assertEquals(7, res.getValidityDays());
        assertEquals(new BigDecimal("29.00"), res.getPrice());
        assertEquals("A", res.getStatus());
    }

    // ── GET ADD-ON BY ID TESTS ────────────────────────────────────────────────

    @Test
    void getAddOnById_whenExists_shouldReturnAddOn() {
        when(repository.findById(1)).thenReturn(Optional.of(mockAddOn));
        AddOnResponse result = service.getAddOnById(1);
        assertNotNull(result);
        assertEquals(1, result.getAddOnId());
        assertEquals("1GB Night Pack", result.getName());
    }

    @Test
    void getAddOnById_whenDoesNotExist_shouldReturnNull() {
        when(repository.findById(999)).thenReturn(Optional.empty());
        AddOnResponse result = service.getAddOnById(999);
        assertNull(result);
    }

    @Test
    void getAddOnById_shouldReturnCorrectStatus() {
        when(repository.findById(1)).thenReturn(Optional.of(mockAddOn));
        AddOnResponse result = service.getAddOnById(1);
        assertEquals("A", result.getStatus());
    }
}