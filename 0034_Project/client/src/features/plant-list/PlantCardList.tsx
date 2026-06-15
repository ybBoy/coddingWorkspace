import React from 'react';
import { Plant, CareType, PLANT_STATUS_OPTIONS } from '../../types';
import styles from '../../styles/plantCardList.module.css';

interface PlantCardListProps {
  plants: Plant[];
  selectedPlantId: string | null;
  onSelectPlant: (plant: Plant) => void;
  onCareAction: (plantId: string, type: CareType) => void;
  onEdit: (plant: Plant) => void;
  onDelete: (plantId: string) => void;
}

const PlantCardList: React.FC<PlantCardListProps> = ({
  plants,
  selectedPlantId,
  onSelectPlant,
  onCareAction,
  onEdit,
  onDelete,
}) => {
  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '从未浇水';
    try {
      if (dateStr.includes('T')) {
        const [datePart] = dateStr.split('T');
        const [year, month, day] = datePart.split('-');
        return `${year}-${month}-${day}`;
      }
      const date = new Date(dateStr);
      if (!isNaN(date.getTime())) {
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
        });
      }
      return dateStr;
    } catch (e) {
      return dateStr;
    }
  };

  const getStatusInfo = (statusLabel: string) => {
    return PLANT_STATUS_OPTIONS.find((s) => s.label === statusLabel) || PLANT_STATUS_OPTIONS[0];
  };

  const getLightIcon = (light: string) => {
    switch (light) {
      case '全日照':
        return '☀️';
      case '半日照':
        return '🌤️';
      case '散射光':
        return '☁️';
      case '耐阴':
        return '🌙';
      default:
        return '🌱';
    }
  };

  const calculateDaysSinceWatering = (lastWatered: string | null) => {
    if (!lastWatered) return null;
    try {
      let lastDate: Date;
      if (lastWatered.includes('T')) {
        const [datePart, timePart] = lastWatered.split('T');
        const [year, month, day] = datePart.split('-');
        const timeMain = timePart.split('.')[0];
        const [hour, minute, second] = timeMain.split(':');
        lastDate = new Date(
          parseInt(year),
          parseInt(month) - 1,
          parseInt(day),
          parseInt(hour),
          parseInt(minute),
          parseInt(second || '0')
        );
      } else {
        lastDate = new Date(lastWatered);
      }
      if (isNaN(lastDate.getTime())) return null;
      const now = new Date();
      const diffTime = now.getTime() - lastDate.getTime();
      const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
      return diffDays;
    } catch (e) {
      return null;
    }
  };

  const calculateNextWatering = (plant: Plant) => {
    if (!plant.lastWateredTime || plant.wateringIntervalDays <= 0) {
      return '立即浇水';
    }
    try {
      let lastDate: Date;
      if (plant.lastWateredTime.includes('T')) {
        const [datePart, timePart] = plant.lastWateredTime.split('T');
        const [year, month, day] = datePart.split('-');
        const timeMain = timePart.split('.')[0];
        const [hour, minute, second] = timeMain.split(':');
        lastDate = new Date(
          parseInt(year),
          parseInt(month) - 1,
          parseInt(day),
          parseInt(hour),
          parseInt(minute),
          parseInt(second || '0')
        );
      } else {
        lastDate = new Date(plant.lastWateredTime);
      }
      if (isNaN(lastDate.getTime())) return '未知';
      
      const nextDate = new Date(lastDate.getTime() + plant.wateringIntervalDays * 24 * 60 * 60 * 1000);
      const now = new Date();
      const diffTime = nextDate.getTime() - now.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays < 0) return `已逾期 ${Math.abs(diffDays)} 天`;
      if (diffDays === 0) return '今天需要浇水';
      if (diffDays === 1) return '明天需要浇水';
      return `${diffDays} 天后浇水`;
    } catch (e) {
      return '未知';
    }
  };

  const getNextWateringColor = (plant: Plant) => {
    if (plant.needsWatering) return styles.nextWateringUrgent;
    const nextWateringText = calculateNextWatering(plant);
    if (nextWateringText.includes('今天') || nextWateringText.includes('明天')) {
      return styles.nextWateringSoon;
    }
    return styles.nextWateringNormal;
  };

  const handleCareAction = (e: React.MouseEvent, plantId: string, type: CareType) => {
    e.stopPropagation();
    onCareAction(plantId, type);
  };

  const handleEdit = (e: React.MouseEvent, plant: Plant) => {
    e.stopPropagation();
    onEdit(plant);
  };

  const handleDelete = (e: React.MouseEvent, plantId: string) => {
    e.stopPropagation();
    if (window.confirm('确定要删除这盆植物吗？相关的养护记录也会一起删除。')) {
      onDelete(plantId);
    }
  };

  if (plants.length === 0) {
    return (
      <div className={styles.emptyState}>
        <div className={styles.emptyIcon}>🌿</div>
        <p className={styles.emptyText}>还没有添加植物</p>
        <p className={styles.emptySubText}>在左侧添加你的第一盆植物吧！</p>
      </div>
    );
  }

  return (
    <div className={styles.cardGrid}>
      {plants.map((plant) => {
        const daysSince = calculateDaysSinceWatering(plant.lastWateredTime);
        const needsWater = plant.needsWatering;
        const statusInfo = getStatusInfo(plant.status);

        return (
          <div
            key={plant.id}
            className={`${styles.card} ${selectedPlantId === plant.id ? styles.selectedCard : ''}`}
            onClick={() => onSelectPlant(plant)}
          >
            {needsWater && (
              <div className={styles.waterAlert}>
                <span className={styles.alertIcon}>💧</span>
                <span className={styles.alertText}>需要浇水</span>
              </div>
            )}

            {plant.photoUrl && (
              <div className={styles.photoContainer}>
                <img src={plant.photoUrl} alt={plant.name} className={styles.plantPhoto} />
              </div>
            )}

            <div className={styles.cardHeader}>
              <h3 className={styles.plantName}>{plant.name}</h3>
              <span
                className={styles.statusBadge}
                style={{ backgroundColor: statusInfo.color + '20', color: statusInfo.color, borderColor: statusInfo.color }}
              >
                {statusInfo.label}
              </span>
            </div>

            <div className={styles.cardBody}>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>📍 位置</span>
                <span className={styles.infoValue}>{plant.location}</span>
              </div>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>{getLightIcon(plant.lightRequirement)} 光照</span>
                <span className={styles.infoValue}>{plant.lightRequirement}</span>
              </div>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>💧 上次浇水</span>
                <span className={styles.infoValue}>
                  {formatDate(plant.lastWateredTime)}
                  {daysSince !== null && (
                    <span className={styles.daysAgo}>（{daysSince} 天前）</span>
                  )}
                </span>
              </div>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>⏰ 下次浇水</span>
                <span className={`${styles.infoValue} ${getNextWateringColor(plant)}`}>
                  {calculateNextWatering(plant)}
                </span>
              </div>
              <div className={styles.infoRow}>
                <span className={styles.infoLabel}>⏱️ 浇水间隔</span>
                <span className={styles.infoValue}>{plant.wateringIntervalDays} 天</span>
              </div>
            </div>

            <div className={styles.cardActions}>
              <button
                className={styles.actionBtn}
                onClick={(e) => handleCareAction(e, plant.id, 'WATERING')}
                title="浇水"
              >
                💧 浇水
              </button>
              <button
                className={styles.actionBtn}
                onClick={(e) => handleCareAction(e, plant.id, 'FERTILIZING')}
                title="施肥"
              >
                🌱 施肥
              </button>
              <button
                className={styles.actionBtn}
                onClick={(e) => handleCareAction(e, plant.id, 'PRUNING')}
                title="修剪"
              >
                ✂️ 修剪
              </button>
            </div>

            <div className={styles.cardFooter}>
              <button
                className={styles.editBtn}
                onClick={(e) => handleEdit(e, plant)}
              >
                编辑
              </button>
              <button
                className={styles.deleteBtn}
                onClick={(e) => handleDelete(e, plant.id)}
              >
                删除
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default PlantCardList;
