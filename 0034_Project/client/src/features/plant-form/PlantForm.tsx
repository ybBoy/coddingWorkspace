import React, { useState, useEffect, useRef } from 'react';
import { Plant, CreatePlantRequest, PLANT_STATUS_OPTIONS } from '../../types';
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
    photoUrl: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [photoPreview, setPhotoPreview] = useState<string>('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (plant) {
      setFormData({
        name: plant.name,
        location: plant.location,
        lightRequirement: plant.lightRequirement,
        status: plant.status,
        wateringIntervalDays: plant.wateringIntervalDays,
        photoUrl: plant.photoUrl || '',
      });
      setPhotoPreview(plant.photoUrl || '');
    } else {
      setFormData({
        name: '',
        location: '',
        lightRequirement: '',
        status: '健康',
        wateringIntervalDays: 7,
        photoUrl: '',
      });
      setPhotoPreview('');
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
          photoUrl: '',
        });
        setPhotoPreview('');
        setErrors({});
      }
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'wateringIntervalDays' ? parseInt(value) || 0 : value,
    }));
  };

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        setErrors((prev) => ({ ...prev, photo: '照片大小不能超过 5MB' }));
        return;
      }
      if (!file.type.startsWith('image/')) {
        setErrors((prev) => ({ ...prev, photo: '请选择图片文件' }));
        return;
      }

      const reader = new FileReader();
      reader.onloadend = () => {
        const base64 = reader.result as string;
        setPhotoPreview(base64);
        setFormData((prev) => ({ ...prev, photoUrl: base64 }));
        setErrors((prev) => {
          const newErrors = { ...prev };
          delete newErrors.photo;
          return newErrors;
        });
      };
      reader.readAsDataURL(file);
    }
  };

  const handleClearPhoto = () => {
    setPhotoPreview('');
    setFormData((prev) => ({ ...prev, photoUrl: '' }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className={styles.formContainer}>
      <h2 className={styles.formTitle}>{plant ? '编辑植物' : '新增植物'}</h2>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formGroup}>
          <label className={styles.label}>植物照片</label>
          <div className={styles.photoUploadArea}>
            {photoPreview ? (
              <div className={styles.photoPreview}>
                <img src={photoPreview} alt="预览" className={styles.previewImage} />
                <button type="button" className={styles.removePhotoBtn} onClick={handleClearPhoto}>
                  ✕ 移除
                </button>
              </div>
            ) : (
              <div className={styles.photoPlaceholder} onClick={() => fileInputRef.current?.click()}>
                <div className={styles.photoIcon}>📷</div>
                <div className={styles.photoText}>点击上传照片</div>
                <div className={styles.photoHint}>Support JPG, PNG, max 5MB</div>
              </div>
            )}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handlePhotoUpload}
              className={styles.hiddenFileInput}
            />
          </div>
          {errors.photo && <span className={styles.error}>{errors.photo}</span>}
        </div>

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
            <option value="全日照">全日照 ☀️</option>
            <option value="半日照">半日照 🌤️</option>
            <option value="散射光">散射光 ☁️</option>
            <option value="耐阴">耐阴 🌙</option>
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
            {PLANT_STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.label}>
                {option.label}
              </option>
            ))}
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
