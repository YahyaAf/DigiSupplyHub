package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.model.Carrier;

public class CarrierMapper {

    private CarrierMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Carrier toEntity(CarrierRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Carrier.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .baseShippingRate(dto.getBaseShippingRate())
                .maxDailyCapacity(dto.getMaxDailyCapacity())
                .cutOffTime(dto.getCutOffTime())
                .currentDailyShipments(0)
                .build();
    }

    public static void updateEntityFromDto(CarrierRequestDto dto, Carrier carrier) {
        if (dto == null || carrier == null) {
            return;
        }

        carrier.setCode(dto.getCode());
        carrier.setName(dto.getName());
        carrier.setContactEmail(dto.getContactEmail());
        carrier.setContactPhone(dto.getContactPhone());
        carrier.setBaseShippingRate(dto.getBaseShippingRate());
        carrier.setMaxDailyCapacity(dto.getMaxDailyCapacity());
        carrier.setCutOffTime(dto.getCutOffTime());
    }

    public static CarrierResponseDto toResponseDto(Carrier carrier) {
        if (carrier == null) {
            return null;
        }

        Integer availableCapacity = null;
        if (carrier.getMaxDailyCapacity() != null) {
            availableCapacity = carrier.getMaxDailyCapacity() - carrier.getCurrentDailyShipments();
        }

        return CarrierResponseDto.builder()
                .id(carrier.getId())
                .code(carrier.getCode())
                .name(carrier.getName())
                .contactEmail(carrier.getContactEmail())
                .contactPhone(carrier.getContactPhone())
                .baseShippingRate(carrier.getBaseShippingRate())
                .maxDailyCapacity(carrier.getMaxDailyCapacity())
                .currentDailyShipments(carrier.getCurrentDailyShipments())
                .cutOffTime(carrier.getCutOffTime())
                .status(carrier.getStatus())
                .availableCapacity(availableCapacity)
                .build();
    }
}