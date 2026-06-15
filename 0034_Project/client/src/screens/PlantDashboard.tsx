import React, { useState, useEffect, useCallback, useRef } from 'react';
import PlantForm from '../features/plant-form/PlantForm';
import PlantCardList from '../features/plant-list/PlantCardList';
import CareLogPanel from '../features/care-log/CareLogPanel';
import CareTimeline from '../features/care-log/CareTimeline';
import StatisticsPanel from '../features/statistics/StatisticsPanel';
import { Plant, CareLog, CareType, CreatePlantRequest, PLANT_STATUS_OPTIONS } from '../types';
import { plantApi } from '../api/plantApi';
import styles from '../styles/dashboard.module.css';

const calculateNeedsWatering = (plant: Plant): boolean => {
  if (!plant.lastWateredTime || plant.wateringIntervalDays <= 0) {
    return true;
  }
  try {
    const dateStr = plant.lastWateredTime;
    let lastWateredDate: Date;

    if (dateStr.includes('T')) {
      const [datePart, timePart] = dateStr.split('T');
      const [year, month, day] = datePart.split('-');
      const timeMain = timePart.split('.')[0];
      const [hour, minute, second] = timeMain.split(':');
      lastWateredDate = new Date(
        parseInt(year),
        parseInt(month) - 1,
        parseInt(day),
        parseInt(hour),
        parseInt(minute),
        parseInt(second || '0')
      );
    } else {
      lastWateredDate = new Date(dateStr);
    }

    if (isNaN(lastWateredDate.getTime())) {
      return true;
    }

    const now = new Date();
    const dueTime = new Date(lastWateredDate.getTime() + plant.wateringIntervalDays * 24 * 60 * 60 * 1000);
    return now.getTime() > dueTime.getTime();
  } catch (e) {
    return true;
  }
};

type SortMode = 'default' | 'urgency';
type DetailView = 'logs' | 'timeline';

const PlantDashboard: React.FC = () => {
  const [plants, setPlants] = useState<Plant[]>([]);
  const [selectedPlant, setSelectedPlant] = useState<Plant | null>(null);
  const [editingPlant, setEditingPlant] = useState<Plant | null>(null);
  const [selectedPlantLogs, setSelectedPlantLogs] = useState<CareLog[]>([]);
  const [locationFilter, setLocationFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [careNoteInput, setCareNoteInput] = useState<string>('');
  const [showCareNoteDialog, setShowCareNoteDialog] = useState<{ plantId: string; type: CareType } | null>(null);
  const [showInfoMessage, setShowInfoMessage] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<SortMode>('default');
  const [showStatistics, setShowStatistics] = useState<boolean>(false);
  const [detailView, setDetailView] = useState<DetailView>('logs');
  const [importError, setImportError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const locations = [...new Set(plants.map((p) => p.location))].filter(Boolean);
  const statuses = PLANT_STATUS_OPTIONS.map((s) => s.label);

  const needsWaterCount = plants.filter((p) => p.needsWatering).length;

  const loadPlants = useCallback(async () => {
    try {
      setLoading(true);
      let data: Plant[];
      if (sortMode === 'urgency') {
        data = await plantApi.getPlantsSortedByUrgency();
      } else {
        data = await plantApi.getAllPlants(locationFilter, statusFilter);
      }
      const plantsWithWaterStatus = data.map((plant) => ({
        ...plant,
        needsWatering: calculateNeedsWatering(plant),
      }));
      setPlants(plantsWithWaterStatus);
      setError(null);
    } catch (err) {
      setError('加载植物列表失败，请检查后端服务是否启动');
      console.error('Error loading plants:', err);
    } finally {
      setLoading(false);
    }
  }, [locationFilter, statusFilter, sortMode]);

  const loadCareLogs = async (plantId: string) => {
    try {
      const logs = await plantApi.getRecentCareLogs(plantId, 5);
      setSelectedPlantLogs(logs);
    } catch (err) {
      console.error('Error loading care logs:', err);
      setSelectedPlantLogs([]);
    }
  };

  useEffect(() => {
    loadPlants();
  }, [loadPlants]);

  useEffect(() => {
    const interval = setInterval(() => {
      setPlants((prev) =>
        prev.map((plant) => ({
          ...plant,
          needsWatering: calculateNeedsWatering(plant),
        }))
      );
    }, 60000);
    return () => clearInterval(interval);
  }, []);

  const handleSelectPlant = async (plant: Plant) => {
    setSelectedPlant(plant);
    setEditingPlant(null);
    await loadCareLogs(plant.id);
  };

  const handleCloseLogs = () => {
    setSelectedPlant(null);
    setSelectedPlantLogs([]);
  };

  const handleCreateOrUpdatePlant = async (data: CreatePlantRequest) => {
    try {
      if (editingPlant) {
        await plantApi.updatePlant(editingPlant.id, data);
        setEditingPlant(null);
      } else {
        await plantApi.createPlant(data);
      }
      await loadPlants();
    } catch (err) {
      setError('保存植物信息失败');
      console.error('Error saving plant:', err);
    }
  };

  const handleEditPlant = (plant: Plant) => {
    setEditingPlant(plant);
    setSelectedPlant(null);
    setSelectedPlantLogs([]);
  };

  const handleDeletePlant = async (plantId: string) => {
    try {
      await plantApi.deletePlant(plantId);
      if (selectedPlant?.id === plantId) {
        setSelectedPlant(null);
        setSelectedPlantLogs([]);
      }
      if (editingPlant?.id === plantId) {
        setEditingPlant(null);
      }
      await loadPlants();
    } catch (err) {
      setError('删除植物失败');
      console.error('Error deleting plant:', err);
    }
  };

  const handleCareAction = (plantId: string, type: CareType) => {
    setShowCareNoteDialog({ plantId, type });
    setCareNoteInput('');
  };

  const confirmCareAction = async () => {
    if (!showCareNoteDialog) return;

    const { plantId, type } = showCareNoteDialog;
    try {
      await plantApi.addCareLog(plantId, type, careNoteInput || '');
      await loadPlants();
      if (selectedPlant?.id === plantId) {
        await loadCareLogs(plantId);
      }
    } catch (err) {
      setError('记录养护动作失败');
      console.error('Error adding care log:', err);
    } finally {
      setShowCareNoteDialog(null);
      setCareNoteInput('');
    }
  };

  const cancelCareAction = () => {
    setShowCareNoteDialog(null);
    setCareNoteInput('');
  };

  const handleClearFilters = () => {
    setLocationFilter('');
    setStatusFilter('');
  };

  const handleShowNeedingWater = () => {
    const needsWater = plants.filter((p) => p.needsWatering);
    if (needsWater.length > 0) {
      const message = `以下 ${needsWater.length} 盆植物需要浇水：\n${needsWater
        .map((p) => `• ${p.name}（${p.location}）`)
        .join('\n')}`;
      setShowInfoMessage(message);
    } else {
      setShowInfoMessage('所有植物都不需要浇水，真棒！🌿');
    }
  };

  const handleExport = async () => {
    try {
      const data = await plantApi.exportPlants();
      const jsonStr = JSON.stringify(data, null, 2);
      const blob = new Blob([jsonStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `plants_backup_${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setShowInfoMessage('导出成功！已下载植物数据 JSON 文件');
    } catch (err) {
      setError('导出失败');
      console.error('Export error:', err);
    }
  };

  const handleImportClick = () => {
    setImportError(null);
    fileInputRef.current?.click();
  };

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const text = await file.text();
      const data = JSON.parse(text);
      if (!Array.isArray(data)) {
        throw new Error('JSON 文件格式不正确，应为数组');
      }
      const result = await plantApi.importPlants(data);
      await loadPlants();
      setShowInfoMessage(result.message || `成功导入 ${data.length} 条植物数据`);
    } catch (err) {
      setImportError(err instanceof Error ? err.message : '导入失败，请检查 JSON 文件格式');
      console.error('Import error:', err);
    } finally {
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleSortChange = (mode: SortMode) => {
    setSortMode(mode);
  };

  return (
    <div className={styles.dashboard}>
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.titleSection}>
            <span className={styles.titleIcon}>🌿</span>
            <h1 className={styles.title}>植物养护记录</h1>
          </div>
          <div className={styles.stats}>
            <span className={styles.statItem}>🪴 共 {plants.length} 盆植物</span>
            {needsWaterCount > 0 && (
              <button className={styles.alertBtn} onClick={handleShowNeedingWater}>
                💧 {needsWaterCount} 盆需要浇水
              </button>
            )}
          </div>
        </div>
        <div className={styles.toolbar}>
          <div className={styles.sortGroup}>
            <span className={styles.sortLabel}>排序：</span>
            <button
              className={`${styles.sortBtn} ${sortMode === 'default' ? styles.sortBtnActive : ''}`}
              onClick={() => handleSortChange('default')}
            >
              默认
            </button>
            <button
              className={`${styles.sortBtn} ${sortMode === 'urgency' ? styles.sortBtnActive : ''}`}
              onClick={() => handleSortChange('urgency')}
            >
              ⚡ 按紧急程度
            </button>
          </div>
          <div className={styles.actionGroup}>
            <button className={styles.toolBtn} onClick={() => setShowStatistics(!showStatistics)}>
              📊 统计
            </button>
            <button className={styles.toolBtn} onClick={handleExport}>
              📤 导出
            </button>
            <button className={styles.toolBtn} onClick={handleImportClick}>
              📥 导入
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".json"
              onChange={handleImportFile}
              className={styles.hiddenFileInput}
            />
          </div>
        </div>
      </header>

      {error && (
        <div className={styles.errorBanner}>
          <span>⚠️ {error}</span>
          <button onClick={() => setError(null)} className={styles.errorClose}>
            ×
          </button>
        </div>
      )}

      {importError && (
        <div className={styles.errorBanner}>
          <span>⚠️ {importError}</span>
          <button onClick={() => setImportError(null)} className={styles.errorClose}>
            ×
          </button>
        </div>
      )}

      <div className={styles.mainContent}>
        <aside className={styles.sidebar}>
          <PlantForm
            plant={editingPlant}
            onSubmit={handleCreateOrUpdatePlant}
            onCancel={editingPlant ? () => setEditingPlant(null) : undefined}
          />

          <div className={styles.filterSection}>
            <h3 className={styles.filterTitle}>🔍 筛选植物</h3>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>按位置</label>
              <select
                value={locationFilter}
                onChange={(e) => setLocationFilter(e.target.value)}
                className={styles.filterSelect}
              >
                <option value="">全部位置</option>
                {locations.map((loc) => (
                  <option key={loc} value={loc}>
                    {loc}
                  </option>
                ))}
              </select>
            </div>

            <div className={styles.filterGroup}>
              <label className={styles.filterLabel}>按状态</label>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className={styles.filterSelect}
              >
                <option value="">全部状态</option>
                {statuses.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
            </div>

            {(locationFilter || statusFilter) && (
              <button onClick={handleClearFilters} className={styles.clearFilterBtn}>
                清除筛选
              </button>
            )}
          </div>

          {showStatistics && (
            <div className={styles.statisticsSection}>
              <StatisticsPanel onClose={() => setShowStatistics(false)} />
            </div>
          )}
        </aside>

        <main className={styles.content}>
          {loading ? (
            <div className={styles.loading}>
              <div className={styles.spinner}></div>
              <p>加载中...</p>
            </div>
          ) : (
            <>
              <PlantCardList
                plants={plants}
                selectedPlantId={selectedPlant?.id || null}
                onSelectPlant={handleSelectPlant}
                onCareAction={handleCareAction}
                onEdit={handleEditPlant}
                onDelete={handleDeletePlant}
              />

              {selectedPlant && (
                <div className={styles.detailPanel}>
                  <div className={styles.detailTabs}>
                    <button
                      className={`${styles.tabBtn} ${detailView === 'logs' ? styles.tabBtnActive : ''}`}
                      onClick={() => setDetailView('logs')}
                    >
                      📝 最近记录
                    </button>
                    <button
                      className={`${styles.tabBtn} ${detailView === 'timeline' ? styles.tabBtnActive : ''}`}
                      onClick={() => setDetailView('timeline')}
                    >
                      📅 养护时间线
                    </button>
                    <button className={styles.closeDetailBtn} onClick={handleCloseLogs}>
                      ✕ 关闭
                    </button>
                  </div>
                  {detailView === 'logs' ? (
                    <CareLogPanel plantName={selectedPlant.name} logs={selectedPlantLogs} onClose={handleCloseLogs} hideHeader />
                  ) : (
                    <CareTimeline plantId={selectedPlant.id} plantName={selectedPlant.name} />
                  )}
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {showCareNoteDialog && (
        <div className={styles.dialogOverlay}>
          <div className={styles.dialog}>
            <h3 className={styles.dialogTitle}>
              记录
              {showCareNoteDialog.type === 'WATERING'
                ? '浇水'
                : showCareNoteDialog.type === 'FERTILIZING'
                ? '施肥'
                : '修剪'}
            </h3>
            <textarea
              className={styles.dialogTextarea}
              placeholder="添加备注（可选）..."
              value={careNoteInput}
              onChange={(e) => setCareNoteInput(e.target.value)}
              rows={3}
            />
            <div className={styles.dialogButtons}>
              <button className={styles.dialogCancelBtn} onClick={cancelCareAction}>
                取消
              </button>
              <button className={styles.dialogConfirmBtn} onClick={confirmCareAction}>
                确认记录
              </button>
            </div>
          </div>
        </div>
      )}

      {showInfoMessage && (
        <div className={styles.dialogOverlay}>
          <div className={styles.dialog}>
            <h3 className={styles.dialogTitle}>💡 提示</h3>
            <p className={styles.dialogMessage}>
              {showInfoMessage.split('\n').map((line, idx) => (
                <span key={idx}>
                  {line}
                  <br />
                </span>
              ))}
            </p>
            <div className={styles.dialogButtons}>
              <button className={styles.dialogConfirmBtn} onClick={() => setShowInfoMessage(null)}>
                知道了
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PlantDashboard;
