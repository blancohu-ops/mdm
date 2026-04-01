package com.industrial.mdm.modules.baseDictionary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import com.industrial.mdm.modules.baseDictionary.repository.AdministrativeRegionRepository;
import com.industrial.mdm.modules.baseDictionary.repository.DictItemRepository;
import com.industrial.mdm.modules.baseDictionary.repository.DictTypeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BaseDictionaryDataPersistenceTest {

    @Autowired
    private BaseDictionaryService baseDictionaryService;

    @Autowired
    private AdministrativeRegionService administrativeRegionService;

    @Autowired
    private DictTypeRepository dictTypeRepository;

    @Autowired
    private DictItemRepository dictItemRepository;

    @Autowired
    private AdministrativeRegionRepository administrativeRegionRepository;

    @Test
    void seedDataIsAvailableForDictionaryQueries() {
        DictTypeResponse companyType = baseDictionaryService.getType("company_type", true);
        DictTypeResponse industry = baseDictionaryService.getType("industry", true);
        DictTypeResponse currency = baseDictionaryService.getType("currency", true);

        assertThat(dictTypeRepository.count()).isEqualTo(8);
        assertThat(dictItemRepository.count()).isEqualTo(79);
        assertThat(companyType.items()).hasSize(5);
        assertThat(industry.items()).hasSize(13);
        assertThat(currency.items()).hasSize(8);
    }

    @Test
    void seedDataIsAvailableForAdministrativeRegionQueries() {
        List<RegionNodeResponse> provinces = administrativeRegionService.listPublicRegions(1, null);
        List<RegionNodeResponse> jiangsuCities =
                administrativeRegionService.listPublicRegions(null, "320000");
        List<RegionNodeResponse> beijingDistricts =
                administrativeRegionService.listPublicRegions(null, "110000");

        assertThat(administrativeRegionRepository.findByLevelOrderBySortOrderAscNameAsc(1)).hasSize(34);
        assertThat(provinces).hasSize(34);
        assertThat(jiangsuCities).hasSize(13);
        assertThat(beijingDistricts).hasSize(16);
    }
}
