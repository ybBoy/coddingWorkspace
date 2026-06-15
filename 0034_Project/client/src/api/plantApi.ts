import { Plant, CareLog, CreatePlantRequest, AddCareLogRequest, UpdateStatusRequest, UpdatePhotoRequest, PlantStatistics } from '../types';

const BASE_URL = 'http://localhost:8088/api';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP error! status: ${response.status}`);
  }
  if (response.status === 204) {
    return {} as T;
  }
  return response.json();
}

export const plantApi = {
  async getAllPlants(location?: string, status?: string): Promise<Plant[]> {
    const params = new URLSearchParams();
    if (location) params.append('location', location);
    if (status) params.append('status', status);
    const query = params.toString() ? `?${params.toString()}` : '';
    const response = await fetch(`${BASE_URL}/plants${query}`);
    return handleResponse<Plant[]>(response);
  },

  async getPlantsSortedByUrgency(): Promise<Plant[]> {
    const response = await fetch(`${BASE_URL}/plants/sorted`);
    return handleResponse<Plant[]>(response);
  },

  async getPlantById(id: string): Promise<Plant> {
    const response = await fetch(`${BASE_URL}/plants/${id}`);
    return handleResponse<Plant>(response);
  },

  async createPlant(plant: CreatePlantRequest): Promise<Plant> {
    const response = await fetch(`${BASE_URL}/plants`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(plant),
    });
    return handleResponse<Plant>(response);
  },

  async updatePlant(id: string, plant: CreatePlantRequest): Promise<Plant> {
    const response = await fetch(`${BASE_URL}/plants/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(plant),
    });
    return handleResponse<Plant>(response);
  },

  async updatePlantStatus(id: string, status: string): Promise<Plant> {
    const body: UpdateStatusRequest = { status };
    const response = await fetch(`${BASE_URL}/plants/${id}/status`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
    return handleResponse<Plant>(response);
  },

  async updatePlantPhoto(id: string, photoUrl: string): Promise<Plant> {
    const body: UpdatePhotoRequest = { photoUrl };
    const response = await fetch(`${BASE_URL}/plants/${id}/photo`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
    return handleResponse<Plant>(response);
  },

  async deletePlant(id: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/plants/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  async addCareLog(plantId: string, type: string, note: string): Promise<CareLog> {
    const body: AddCareLogRequest = { type: type as any, note };
    const response = await fetch(`${BASE_URL}/plants/${plantId}/care-logs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
    return handleResponse<CareLog>(response);
  },

  async getRecentCareLogs(plantId: string, count: number = 5): Promise<CareLog[]> {
    const response = await fetch(`${BASE_URL}/plants/${plantId}/care-logs/recent?count=${count}`);
    return handleResponse<CareLog[]>(response);
  },

  async getCareTimeline(plantId: string): Promise<Record<string, CareLog[]>> {
    const response = await fetch(`${BASE_URL}/plants/${plantId}/timeline`);
    return handleResponse<Record<string, CareLog[]>>(response);
  },

  async getPlantsNeedingWater(): Promise<Plant[]> {
    const response = await fetch(`${BASE_URL}/plants/needing-water`);
    return handleResponse<Plant[]>(response);
  },

  async getStatistics(): Promise<PlantStatistics> {
    const response = await fetch(`${BASE_URL}/statistics`);
    return handleResponse<PlantStatistics>(response);
  },

  async exportPlants(): Promise<Plant[]> {
    const response = await fetch(`${BASE_URL}/plants/export`);
    return handleResponse<Plant[]>(response);
  },

  async importPlants(plants: Plant[]): Promise<{ message: string }> {
    const response = await fetch(`${BASE_URL}/plants/import`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(plants),
    });
    return handleResponse<{ message: string }>(response);
  },
};

export default plantApi;
