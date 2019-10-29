package org.meveo.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.CustomFieldMatrixColumnDto;
import org.meveo.api.dto.CustomFieldTemplateDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldMapKeyEnum;
import org.meveo.model.crm.custom.CustomFieldMatrixColumn;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.customEntities.CustomRelationshipTemplate;
import org.meveo.model.persistence.DBStorageType;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.crm.impl.SampleValueHelper;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.CustomRelationshipTemplateService;
import org.meveo.service.custom.CustomizedEntity;
import org.meveo.service.custom.CustomizedEntityService;
import org.meveo.util.EntityCustomizationUtils;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class CustomFieldTemplateApi extends BaseApi {

    @Inject
    private CalendarService calendarService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    @Inject
    private CustomizedEntityService customizedEntityService;

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private CustomRelationshipTemplateService customRelationshipTemplateService;
    
    private String displayFormat;

    public void create(CustomFieldTemplateDto postData, String appliesTo) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getDescription())) {
            missingParameters.add("description");
        }
        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }
        if (postData.getFieldType() == null) {
            missingParameters.add("fieldType");
        }
        if (postData.getStorageType() == null) {
            missingParameters.add("storageType");
        }
        if (postData.getStorageType() == CustomFieldStorageTypeEnum.MATRIX && (postData.getMatrixColumns() == null || postData.getMatrixColumns().isEmpty())) {
            missingParameters.add("matrixColumns");
            
        if(postData.getFieldType() == CustomFieldTypeEnum.ENTITY && postData.getStorages().contains(DBStorageType.NEO4J) && postData.getRelationshipName() == null){
        	 missingParameters.add("relationshipName");
        }
        
        } else if (postData.getStorageType() == CustomFieldStorageTypeEnum.MATRIX) {
            for (CustomFieldMatrixColumnDto columnDto : postData.getMatrixColumns()) {
                if (StringUtils.isBlank(columnDto.getCode())) {
                    missingParameters.add("matrixColumns/code");
                }
                if (StringUtils.isBlank(columnDto.getLabel())) {
                    missingParameters.add("matrixColumns/label");
                }
                if (columnDto.getKeyType() == null) {
                    missingParameters.add("matrixColumns/keyType");
                }
            }
        }

        if (postData.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY
                && (postData.getStorageType() != CustomFieldStorageTypeEnum.LIST || (postData.isVersionable() != null && postData.isVersionable()))) {
            throw new InvalidParameterException("Custom field of type CHILD_ENTITY only supports unversioned values and storage type of LIST");
        }
        if (postData.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY
                && (postData.getChildEntityFieldsForSummary() == null || postData.getChildEntityFieldsForSummary().isEmpty())) {
            missingParameters.add("childEntityFieldsForSummary");
        }
        handleMissingParameters();

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {
            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        if (!getCustomizedEntitiesAppliesTo().contains(appliesTo)) {
            throw new InvalidParameterException("appliesTo", appliesTo);
        }

        if (customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo) != null) {
            throw new EntityAlreadyExistsException(CustomFieldTemplate.class, postData.getCode());
        }

        CustomFieldTemplate cft = fromDTO(postData, appliesTo, null);
        
        validateSamples(cft);
        customFieldTemplateService.create(cft);

    }

    public void update(CustomFieldTemplateDto postData, String appliesTo) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }
        
        if(postData.getFieldType() == CustomFieldTypeEnum.ENTITY && postData.getStorages() != null && postData.getStorages().contains(DBStorageType.NEO4J) && postData.getRelationshipName() == null){
       	 	missingParameters.add("relationshipName");
        }

        if (postData.getMatrixColumns() != null) {
            for (CustomFieldMatrixColumnDto columnDto : postData.getMatrixColumns()) {
                if (StringUtils.isBlank(columnDto.getCode())) {
                    missingParameters.add("matrixColumns/code");
                }
                if (StringUtils.isBlank(columnDto.getLabel())) {
                    missingParameters.add("matrixColumns/label");
                }
                if (columnDto.getKeyType() == null) {
                    missingParameters.add("matrixColumns/keyType");
                }
            }
        }

        handleMissingParameters();

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {
            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        if (!getCustomizedEntitiesAppliesTo().contains(appliesTo)) {
            throw new InvalidParameterException("appliesTo", appliesTo);
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo);
        if (cft == null) {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, postData.getCode());
        }

        if (cft.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY && postData.isVersionable() != null && postData.isVersionable()) {
            throw new InvalidParameterException("Custom field of type CHILD_ENTITY only supports unversioned values and storage type of LIST");
        }
        if (cft.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY && (cft.getChildEntityFields() == null || postData.getChildEntityFieldsForSummary().isEmpty())) {
            missingParameters.add("childEntityFieldsForSummary");
        }

        cft = fromDTO(postData, appliesTo, cft);

        validateSamples(cft);

        customFieldTemplateService.update(cft);

    }

    public void remove(String code, String appliesTo) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(appliesTo)) {
            missingParameters.add("appliesTo");
        }

        handleMissingParameters();

        if (!getCustomizedEntitiesAppliesTo().contains(appliesTo)) {
            throw new InvalidParameterException("appliesTo", appliesTo);
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesTo(code, appliesTo);
        if (cft != null) {
            customFieldTemplateService.remove(cft.getId());
        } else {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, code);
        }
    }

    /**
     * Find Custom Field Template by its code and appliesTo attributes.
     * 
     * @param code Custom Field Template code
     * @param appliesTo Applies to
     * @return DTO
     * @throws EntityDoesNotExistsException Custom Field Template was not found
     * @throws InvalidParameterException AppliesTo value is incorrect
     * @throws MissingParameterException A parameter, necessary to find an Custom Field Template, was not provided
     */
    public CustomFieldTemplateDto find(String code, String appliesTo) throws EntityDoesNotExistsException, MissingParameterException, InvalidParameterException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(appliesTo)) {
            missingParameters.add("appliesTo");
        }

        handleMissingParameters();

        if (!getCustomizedEntitiesAppliesTo().contains(appliesTo)) {
            throw new InvalidParameterException("appliesTo", appliesTo);
        }

        CustomFieldTemplate cft = customFieldTemplateService.findByCodeAndAppliesToNoCache(code, appliesTo);

        if (cft == null) {
            throw new EntityDoesNotExistsException(CustomFieldTemplate.class, code + "/" + appliesTo);
        }
        return new CustomFieldTemplateDto(cft);
    }

    /**
     * Same as find method, only ignore EntityDoesNotExistException exception and return Null instead.
     * 
     * @param code Custom Field Template code
     * @param appliesTo Applies to
     * @return DTO or Null if not found
     * @throws InvalidParameterException AppliesTo value is incorrect
     * @throws MissingParameterException A parameter, necessary to find an Custom Field Template, was not provided
     */
    public CustomFieldTemplateDto findIgnoreNotFound(String code, String appliesTo) throws MissingParameterException, InvalidParameterException {
        try {
            return find(code, appliesTo);
        } catch (EntityDoesNotExistsException e) {
            return null;
        }
    }

    public void createOrUpdate(CustomFieldTemplateDto postData, String appliesTo) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }

        if (appliesTo == null && StringUtils.isBlank(postData.getAccountLevel()) && StringUtils.isBlank(postData.getAppliesTo())) {
            missingParameters.add("appliesTo");
        }

        handleMissingParameters();

        if (appliesTo != null) {
            postData.setAppliesTo(appliesTo);

        } else {
            // Support for old API
            if (postData.getAppliesTo() == null && postData.getAccountLevel() != null) {
                appliesTo = postData.getAccountLevel();
            } else {
                appliesTo = postData.getAppliesTo();
            }
        }

        CustomFieldTemplate customFieldTemplate = customFieldTemplateService.findByCodeAndAppliesToNoCache(postData.getCode(), appliesTo);

        if (customFieldTemplate == null) {
            create(postData, appliesTo);
        } else {
            update(postData, appliesTo);
        }
    }
    
    public String getDisplayFormat() {
        return displayFormat;
    }

    public void setDisplayFormat(String displayFormat) {
        this.displayFormat = displayFormat;
    }

    protected CustomFieldTemplate fromDTO(CustomFieldTemplateDto dto, String appliesTo, CustomFieldTemplate cftToUpdate) throws InvalidParameterException {

        // Set default values
        if (dto.getFieldType() == CustomFieldTypeEnum.STRING && dto.getMaxValue() == null) {
            dto.setMaxValue(CustomFieldTemplate.DEFAULT_MAX_LENGTH_STRING);
        }

        CustomFieldTemplate cft = cftToUpdate;
        if (cftToUpdate == null) {
            cft = new CustomFieldTemplate();
            cft.setCode(dto.getCode());
            cft.setFieldType(dto.getFieldType());
            cft.setStorageType(dto.getStorageType());
            if (appliesTo == null) {

                // Support for old API
                if (dto.getAccountLevel() != null) {
                    appliesTo = dto.getAccountLevel();
                } else {
                    appliesTo = dto.getAppliesTo();
                }
            }
            cft.setAppliesTo(appliesTo);
        }

        if (dto.getDescription() != null) {
            cft.setDescription(dto.getDescription());
        }
        
        cft.setRelationshipName(dto.getRelationshipName());

        if (dto.getDefaultValue() != null) {
            cft.setDefaultValue(dto.getDefaultValue());
        }
        if (dto.isUseInheritedAsDefaultValue() != null) {
            cft.setUseInheritedAsDefaultValue(dto.isUseInheritedAsDefaultValue());
        }
        if (dto.isValueRequired() != null) {
            cft.setValueRequired(dto.isValueRequired());
        }
        if (dto.isVersionable() != null) {
            cft.setVersionable(dto.isVersionable());
        }
        if (dto.isTriggerEndPeriodEvent() != null) {
            cft.setTriggerEndPeriodEvent(dto.isTriggerEndPeriodEvent());
        }
        if (dto.getEntityClazz() != null) {
            cft.setEntityClazz(StringUtils.trimToNull(dto.getEntityClazz()));
        }
        if (dto.isAllowEdit() != null) {
            cft.setAllowEdit(dto.isAllowEdit());
        }
        if (dto.isHideOnNew() != null) {
            cft.setHideOnNew(dto.isHideOnNew());
        }
        if (dto.getMinValue() != null) {
            cft.setMinValue(dto.getMinValue());
        }
        if (dto.getMaxValue() != null) {
            cft.setMaxValue(dto.getMaxValue());
        }
        if (dto.getRegExp() != null) {
            cft.setRegExp(dto.getRegExp());
        }
        if (dto.getGuiPosition() != null) {
            cft.setGuiPosition(dto.getGuiPosition());
        }
        if (dto.getApplicableOnEl() != null) {
            cft.setApplicableOnEl(dto.getApplicableOnEl());
        }

        if (cft.getFieldType() == CustomFieldTypeEnum.LIST && dto.getListValues() != null) {
            cft.setListValues(dto.getListValues());
        }

        if (dto.getMapKeyType() != null) {
            cft.setMapKeyType(dto.getMapKeyType());
        }
        if (dto.getIndexType() != null) {
            cft.setIndexType(dto.getIndexType());
        }
        if (dto.getTags() != null) {
            cft.setTags(dto.getTags());
        }
        if (cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == null) {
            cft.setMapKeyType(CustomFieldMapKeyEnum.STRING);
        }

        if (cft.getStorageType() == CustomFieldStorageTypeEnum.MATRIX && dto.getMatrixColumns() != null) {
            if (cft.getMatrixColumns() == null) {
                cft.setMatrixColumns(new ArrayList<CustomFieldMatrixColumn>());
            } else {
                cft.getMatrixColumns().clear();
            }

            for (CustomFieldMatrixColumnDto columnDto : dto.getMatrixColumns()) {
                cft.getMatrixColumns().add(CustomFieldMatrixColumnDto.fromDto(columnDto));
            }
        }

        if (cft.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY) {
            cft.setStorageType(CustomFieldStorageTypeEnum.LIST);
            cft.setVersionable(false);
            if (dto.getChildEntityFieldsForSummary() != null) {
                cft.setChildEntityFieldsAsList(dto.getChildEntityFieldsForSummary());
            }
        }

        if (dto.getCalendar() != null) {
            if (StringUtils.isBlank(dto.getCalendar())) {
                cft.setCalendar(null);
            } else {
                Calendar calendar = calendarService.findByCode(dto.getCalendar());
                if (calendar != null) {
                    cft.setCalendar(calendar);
                } else {
                    cft.setCalendar(null);
                }
            }
        }

        if (dto.getLanguageDescriptions() != null) {
            cft.setDescriptionI18n(convertMultiLanguageToMapOfValues(dto.getLanguageDescriptions(), cft.getDescriptionI18n()));
        }

        cft.setUnique(dto.isUnique());

        cft.setIdentifier(dto.isIdentifier());

        // A cft can't be stored in a db that is not available for its cet
        List<DBStorageType> storageTypes = null;
        if(cft.getAppliesTo().startsWith(CustomEntityTemplate.CFT_PREFIX)) {
            String cetCode = CustomEntityTemplate.getCodeFromAppliesTo(cft.getAppliesTo());
            CustomEntityTemplate cet = customEntityTemplateService.findByCode(cetCode);
            storageTypes = cet.getAvailableStorages();
        }else if(cft.getAppliesTo().startsWith(CustomRelationshipTemplate.CRT_PREFIX)) {
            String crtCode = EntityCustomizationUtils.getEntityCode(cft.getAppliesTo());
            CustomRelationshipTemplate crt = customRelationshipTemplateService.findByCode(crtCode);
            storageTypes = crt.getAvailableStorages();
        }

        if(dto.getStorages() != null) {
            for (DBStorageType storageType : dto.getStorages()) {
                if (storageTypes == null || !storageTypes.contains(storageType)) {
                    String message = "Custom field %s can't be stored to %s as the CET / CRT with code %s is not configure to be stored in this database";
                    throw new InvalidParameterException(String.format(message, cft.getCode(), storageType, EntityCustomizationUtils.getEntityCode(cft.getAppliesTo())));
                }
            }
        }
        if (dto.getDisplayFormat() != null) {
            cft.setDisplayFormat(dto.getDisplayFormat());
        }

        if (dto.getSamples() != null) {
            cft.setSamples(dto.getSamples());
        }

        cft.setStorages(dto.getStorages());
        cft.setSummary(dto.isSummary());

        cft.setFileExtensions(dto.getFileExtensions());
        cft.setContentTypes(dto.getContentTypes());
        cft.setMaxFileSizeAllowedInKb(dto.getMaxFileSizeAllowedInKb());
        cft.setFilePath(dto.getFilePath());
        cft.setSaveOnExplorer(dto.isSaveOnExplorer());

        return cft;
    }

    private List<String> getCustomizedEntitiesAppliesTo() {
        List<String> cftAppliesto = new ArrayList<String>();
        List<CustomizedEntity> entities = customizedEntityService.getCustomizedEntities(null, false, true, true, null, null);
        for (CustomizedEntity customizedEntity : entities) {
            cftAppliesto.add(EntityCustomizationUtils.getAppliesTo(customizedEntity.getEntityClass(), customizedEntity.getEntityCode()));
        }
        return cftAppliesto;
    }

	@SuppressWarnings("unused")
	private void validateSamples(CustomFieldTemplate template) {
		
		if(template.getSamples() == null || template.getSamples().isEmpty()) {
			return;
		}

		if (template.getFieldType() == CustomFieldTypeEnum.STRING) {
			Map<Integer, String> validateSamples = SampleValueHelper.validateStringType(template.getSamples(), template.getStorageType());
			if (!validateSamples.isEmpty()) {
				for (Map.Entry<Integer, String> validateSample : validateSamples.entrySet()) {
					throw new IllegalArgumentException();
				}
			}
		}

		if (template.getFieldType() == CustomFieldTypeEnum.LONG) {
			Map<Integer, String> validateSamples = SampleValueHelper.validateLongType(template.getSamples(), template.getStorageType());
			if (!validateSamples.isEmpty()) {
				for (Map.Entry<Integer, String> validateSample : validateSamples.entrySet()) {
					throw new IllegalArgumentException();
				}
			}
		}

		if (template.getFieldType() == CustomFieldTypeEnum.DOUBLE) {
			Map<Integer, String> validateSamples = SampleValueHelper.validateDoubleType(template.getSamples(), template.getStorageType());
			if (!validateSamples.isEmpty()) {
				for (Map.Entry<Integer, String> validateSample : validateSamples.entrySet()) {
					throw new IllegalArgumentException();
				}
			}
		}

		if (template.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY) {
			Map<String, CustomFieldTemplate> customFieldTemplates = customFieldTemplateService.findByAppliesTo(CustomEntityTemplate.getAppliesTo(template.getEntityClazzCetCode()));
			Map<Integer, String> validateSamples = SampleValueHelper.validateChildEntityType(customFieldTemplates, template.getSamples(), template.getStorageType());
			if (!validateSamples.isEmpty()) {
				for (Map.Entry<Integer, String> validateSample : validateSamples.entrySet()) {
					throw new IllegalArgumentException();
				}
			}
		}

	}
}