import React, { useState, useEffect } from 'react';
import { Plant, CreatePlantRequest } from '../../types';
import styles from '../../styles/plantForm.module.css';

interface PlantFormProps {
  plant?: Plant | null;
  onSubmit: (data: CreatePlantRequest) => void;
  onCancel?: () => void;
}

const PlantForm: React.FC<PlantFormProps> = ({ plant, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState<CreatePlantRequest>({
    name: '',
    location: '',
    lightRequirement: '',
    status: '健康',
    wateringIntervalDays: 7,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (plant) {
      setFormData({
        name: plant.name,
        location: plant.location,
        lightRequirement: plant.lightRequirement,
        status: plant.status,
        wateringIntervalDays: plant.wateringIntervalDays,
      });
    } else {
      setFormData({
        name: '',
        location: '',
        lightRequirement: '',
        status: '健康',
        wateringIntervalDays: 7,
      });
    }
  }, [plant]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim()) {
      newErrors.name = '请输入植物名称';
    }
    if (!formData.location.trim()) {
      newErrors.location = '请输入位置';
    }
    if (!formData.lightRequirement.trim()) {
      newErrors.lightRequirement = '请选择光照需求';
    }
    if (formData.wateringIntervalDays < 1) {
      newErrors.wateringIntervalDays = '浇水间隔至少1天';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      onSubmit(formData);
      if (!plant) {
        setFormData({
          name: '',
          location: '',
          lightRequirement: '',
          status: '健康',
          wateringIntervalDays: 7,
        });
        setErrors({});
      }
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'wateringIntervalDays' ? parseInt(value) || 0 : value,
    }));
  };

  return (
    <div className={styles.formContainer}>
      <h2 className={styles.formTitle}>{plant ? '编辑植物' : '新增植物'}</h2>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formGroup}>
          <label className={styles.label}>植物名称 *</label>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            className={`${styles.input} ${errors.name ? styles.inputError : ''}`}
            placeholder="如：绿萝"
          />
          {errors.name && <span className={styles.error}>{errors.name}</span>}
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>位置 *</label>
          <input
            type="text"
            name="location"
            value={formData.location}
            onChange={handleChange}
            className={`${styles.input} ${errors.location ? styles.inputError : ''}`}
            placeholder="如：客厅阳台"
          />
          {errors.location && <span className={styles.error}>{errors.location}</span>}
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>光照需求 *</label>
          <select
            name="lightRequirement"
            value={formData.lightRequirement}
            onChange={handleChange}
            className={`${styles.select} ${errors.lightRequirement ? styles.inputError : ''}`}
          >
            <option value="">请选择</option>
            <option value="全日照">全日照</option>
            <option value="半日照">半日照</option>
            <option value="散射光">散射光</option>
            <option value="耐阴">耐阴</option>
          </select>
          {errors.lightRequirement && <span className={styles.error}>{errors.lightRequirement}</span>}
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>当前状态</label>
          <select
            name="status"
            value={formData.status}
            onChange={handleChange}
            className={styles.select}
          >
            <option value="健康">健康</option>
            <option value="生长良好">生长良好</option>
            <option value="需要关注">需要关注</option>
            <option value="生病">生病</option>
            <option value="休眠">休眠</option>
          </select>
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>浇水间隔（天）*</label>
          <input
            type="number"
            name="wateringIntervalDays"
            min="1"
            max="60"
            value={formData.wateringIntervalDays}
            onChange={handleChange}
            className={`${styles.input} ${errors.wateringIntervalDays ? styles.inputError : ''}`}
          />
          {errors.wateringIntervalDays && <span className={styles.error}>{errors.wateringIntervalDays}</span>}
        </div>

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.submitButton}>
            {plant ? '保存修改' : '添加植物'}
          </button>
          {onCancel && (
            <button type="button" onClick={onCancel} className={styles.cancelButton}>
              取消
            </button>
          )}
        </div>
      </form>
    </div>
  );
};

export default PlantForm;
