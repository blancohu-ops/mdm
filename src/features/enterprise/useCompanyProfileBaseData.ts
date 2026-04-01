import { useEffect, useState } from "react";
import { enterpriseService } from "@/services/enterpriseService";
import { dictionaryService } from "@/services/dictionaryService";
import type { CompanyProfileFormState } from "@/features/enterprise/companyProfileForm";
import type { DictItem, RegionNode } from "@/types/dictionary";

type CompanyProfileBaseData = {
  companyTypes: DictItem[];
  industries: DictItem[];
  mainCategories: string[];
  provinces: RegionNode[];
  cities: RegionNode[];
  districts: RegionNode[];
  error: string;
};

export function useCompanyProfileBaseData(
  form: CompanyProfileFormState | null,
): CompanyProfileBaseData {
  const [companyTypes, setCompanyTypes] = useState<DictItem[]>([]);
  const [industries, setIndustries] = useState<DictItem[]>([]);
  const [mainCategories, setMainCategories] = useState<string[]>([]);
  const [provinces, setProvinces] = useState<RegionNode[]>([]);
  const [cities, setCities] = useState<RegionNode[]>([]);
  const [districts, setDistricts] = useState<RegionNode[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([
      dictionaryService.fetchEnabledDictItems("company_type"),
      dictionaryService.fetchEnabledDictItems("industry"),
      enterpriseService.fetchCategoryLeafOptions(),
      dictionaryService.fetchEnabledRegions({ level: 1 }),
    ])
      .then(([companyTypeResult, industryResult, categoryResult, provinceResult]) => {
        if (!active) {
          return;
        }

        setCompanyTypes(companyTypeResult.data);
        setIndustries(industryResult.data);
        setMainCategories(categoryResult.data);
        setProvinces(provinceResult.data);
        setError("");
      })
      .catch((serviceError) => {
        if (!active) {
          return;
        }

        setError(
          serviceError instanceof Error
            ? serviceError.message
            : "基础资料选项加载失败",
        );
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!form?.province.trim()) {
      setCities([]);
      setDistricts([]);
      return;
    }

    const selectedProvince = provinces.find((item) => item.name === form.province);
    if (!selectedProvince) {
      setCities([]);
      setDistricts([]);
      return;
    }

    let active = true;

    dictionaryService
      .fetchEnabledRegions({ parentCode: selectedProvince.code })
      .then((result) => {
        if (!active) {
          return;
        }

        setCities(result.data);
        setError("");
      })
      .catch((serviceError) => {
        if (!active) {
          return;
        }

        setCities([]);
        setDistricts([]);
        setError(
          serviceError instanceof Error
            ? serviceError.message
            : "城市选项加载失败",
        );
      });

    return () => {
      active = false;
    };
  }, [form?.province, provinces]);

  useEffect(() => {
    if (!form?.city.trim()) {
      setDistricts([]);
      return;
    }

    const selectedCity = cities.find((item) => item.name === form.city);
    if (!selectedCity) {
      setDistricts([]);
      return;
    }

    let active = true;

    dictionaryService
      .fetchEnabledRegions({ parentCode: selectedCity.code })
      .then((result) => {
        if (!active) {
          return;
        }

        setDistricts(result.data);
        setError("");
      })
      .catch((serviceError) => {
        if (!active) {
          return;
        }

        setDistricts([]);
        setError(
          serviceError instanceof Error
            ? serviceError.message
            : "区县选项加载失败",
        );
      });

    return () => {
      active = false;
    };
  }, [cities, form?.city]);

  return {
    companyTypes,
    industries,
    mainCategories,
    provinces,
    cities,
    districts,
    error,
  };
}
