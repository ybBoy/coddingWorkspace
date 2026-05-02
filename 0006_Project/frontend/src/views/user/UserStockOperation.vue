<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-upload2" style="margin-right: 10px;"></i>入库/出库操作
    </div>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="operation-card">
          <div slot="header">
            <span class="card-title">
              <i class="el-icon-circle-plus" style="color: #67c23a; margin-right: 8px;"></i>
              入库操作
            </span>
          </div>
          <el-form :model="stockInForm" :rules="rules" ref="stockInFormRef" label-width="100px">
            <el-form-item label="选择零件" prop="partId">
              <el-select
              v-model="stockInForm.partId"
              filterable
              placeholder="请选择要入库的零件"
              style="width: 100%"
              @change="handlePartSelect('in', $event)"
            >
              <el-option
                v-for="part in parts"
                :key="part.id"
                :label="part.name + ' (' + part.id + ') - 库存: ' + part.quantity + ' ' + part.unit"
                :value="part.id"
              >
                <span style="float: left">{{ part.name }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">
                  库存: {{ part.quantity }} {{ part.unit }}
                </span>
              </el-option>
            </el-select>
            </el-form-item>
            <el-form-item label="零件信息">
              <div v-if="selectedStockInPart" class="part-info">
                <p><strong>编号：</strong>{{ selectedStockInPart.id }}</p>
                <p><strong>名称：</strong>{{ selectedStockInPart.name }}</p>
                <p><strong>规格：</strong>{{ selectedStockInPart.specification }}</p>
                <p><strong>当前库存：</strong>
                  <span class="stock-quantity">{{ selectedStockInPart.quantity }} {{ selectedStockInPart.unit }}</span>
                </p>
                <p><strong>最低库存：</strong>{{ selectedStockInPart.minStock }} {{ selectedStockInPart.unit }}</p>
                <p><strong>状态：</strong>
                  <el-tag :type="selectedStockInPart.quantity <= selectedStockInPart.minStock ? 'danger' : 'success'">
                    {{ selectedStockInPart.quantity <= selectedStockInPart.minStock ? '需补货' : '正常' }}
                  </el-tag>
                </p>
              </div>
              <el-empty v-else description="请先选择零件" :image-size="80"></el-empty>
            </el-form-item>
            <el-form-item label="入库数量" prop="quantity">
              <el-input-number v-model="stockInForm.quantity" :min="1" style="width: 200px"></el-input-number>
              <span style="margin-left: 10px;">{{ selectedStockInPart ? selectedStockInPart.unit : '单位' }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="success" @click="handleStockIn" :loading="operating">
                <i class="el-icon-circle-plus"></i> 确认入库
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="operation-card">
          <div slot="header">
            <span class="card-title">
              <i class="el-icon-remove" style="color: #f56c6c; margin-right: 8px;"></i>
              出库操作
            </span>
          </div>
          <el-form :model="stockOutForm" :rules="rules" ref="stockOutFormRef" label-width="100px">
            <el-form-item label="选择零件" prop="partId">
              <el-select
              v-model="stockOutForm.partId"
              filterable
              placeholder="请选择要出库的零件"
              style="width: 100%"
              @change="handlePartSelect('out', $event)"
            >
              <el-option
                v-for="part in parts.filter(p => p.quantity > 0)"
                :key="part.id"
                :label="part.name + ' (' + part.id + ') - 库存: ' + part.quantity + ' ' + part.unit"
                :value="part.id"
              >
                <span style="float: left">{{ part.name }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">
                  库存: {{ part.quantity }} {{ part.unit }}
                </span>
              </el-option>
            </el-select>
            </el-form-item>
            <el-form-item label="零件信息">
              <div v-if="selectedStockOutPart" class="part-info">
                <p><strong>编号：</strong>{{ selectedStockOutPart.id }}</p>
                <p><strong>名称：</strong>{{ selectedStockOutPart.name }}</p>
                <p><strong>规格：</strong>{{ selectedStockOutPart.specification }}</p>
                <p><strong>当前库存：</strong>
                  <span class="stock-quantity">{{ selectedStockOutPart.quantity }} {{ selectedStockOutPart.unit }}</span>
                </p>
                <p><strong>最低库存：</strong>{{ selectedStockOutPart.minStock }} {{ selectedStockOutPart.unit }}</p>
                <p><strong>状态：</strong>
                  <el-tag :type="selectedStockOutPart.quantity <= selectedStockOutPart.minStock ? 'danger' : 'success'">
                    {{ selectedStockOutPart.quantity <= selectedStockOutPart.minStock ? '需补货' : '正常' }}
                  </el-tag>
                </p>
              </div>
              <el-empty v-else description="请先选择零件" :image-size="80"></el-empty>
            </el-form-item>
            <el-form-item label="出库数量" prop="quantity">
              <el-input-number
                v-model="stockOutForm.quantity"
                :min="1"
                :max="selectedStockOutPart ? selectedStockOutPart.quantity : 99999"
                style="width: 200px"
              ></el-input-number>
              <span style="margin-left: 10px;">{{ selectedStockOutPart ? selectedStockOutPart.unit : '单位' }}</span>
              <span v-if="selectedStockOutPart" style="margin-left: 10px; color: #909399; font-size: 12px;">
                (最大可出库: {{ selectedStockOutPart.quantity }})
              </span>
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="handleStockOut" :loading="operating">
                <i class="el-icon-remove"></i> 确认出库
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <div class="recent-operations" v-if="recentOperations.length > 0">
      <div class="section-title">最近操作记录</div>
      <el-table :data="recentOperations" style="width: 100%">
        <el-table-column prop="type" label="操作类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === '入库' ? 'success' : 'danger'">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="partId" label="零件编号" width="120"></el-table-column>
        <el-table-column prop="partName" label="零件名称"></el-table-column>
        <el-table-column prop="quantity" label="数量" width="100">
          <template slot-scope="scope">
            <span :class="scope.row.type === '入库' ? 'success-text' : 'danger-text'">
              {{ scope.row.type === '入库' ? '+' : '-' }}{{ scope.row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80"></el-table-column>
        <el-table-column prop="time" label="操作时间" width="180"></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import partApi from '../../api/partApi'
import { eventBus } from '../../utils/eventBus'

export default {
  name: 'UserStockOperation',
  data() {
    return {
      loading: false,
      operating: false,
      parts: [],
      selectedStockInPart: null,
      selectedStockOutPart: null,
      stockInForm: {
        partId: '',
        quantity: 1
      },
      stockOutForm: {
        partId: '',
        quantity: 1
      },
      rules: {
        partId: [{ required: true, message: '请选择零件', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
      },
      recentOperations: []
    }
  },
  created() {
    this.loadParts()
  },
  methods: {
    async loadParts() {
      this.loading = true
      try {
        const res = await partApi.user.getVisibleParts()
        if (res.success) {
          this.parts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载零件列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    handlePartSelect(type, partId) {
      const part = this.parts.find(p => p.id === partId)
      if (type === 'in') {
        this.selectedStockInPart = part
        this.stockInForm.quantity = 1
      } else {
        this.selectedStockOutPart = part
        this.stockOutForm.quantity = 1
      }
    },

    async handleStockIn() {
      this.$refs.stockInFormRef.validate(async (valid) => {
        if (valid) {
          this.operating = true
          try {
            const res = await partApi.user.stockIn(this.stockInForm.partId, this.stockInForm.quantity)
            if (res.success) {
              this.$message.success('入库成功，新增 ' + this.stockInForm.quantity + ' ' + this.selectedStockInPart.unit)
              eventBus.$emit('stock-changed')
              this.addOperation('入库', this.selectedStockInPart, this.stockInForm.quantity)
              this.loadParts()
              this.resetForm('in')
            } else {
              this.$message.error(res.message)
            }
          } catch (error) {
            this.$message.error('入库失败: ' + error.message)
          } finally {
            this.operating = false
          }
        }
      })
    },

    async handleStockOut() {
      this.$refs.stockOutFormRef.validate(async (valid) => {
        if (valid) {
          if (this.stockOutForm.quantity > this.selectedStockOutPart.quantity) {
            this.$message.error('出库数量不能超过当前库存')
            return
          }
          
          this.operating = true
          try {
            const res = await partApi.user.stockOut(this.stockOutForm.partId, this.stockOutForm.quantity)
            if (res.success) {
              this.$message.success('出库成功，减少 ' + this.stockOutForm.quantity + ' ' + this.selectedStockOutPart.unit)
              eventBus.$emit('stock-changed')
              this.addOperation('出库', this.selectedStockOutPart, this.stockOutForm.quantity)
              this.loadParts()
              this.resetForm('out')
            } else {
              this.$message.error(res.message)
            }
          } catch (error) {
            this.$message.error('出库失败: ' + error.message)
          } finally {
            this.operating = false
          }
        }
      })
    },

    addOperation(type, part, quantity) {
      const operation = {
        type: type,
        partId: part.id,
        partName: part.name,
        quantity: quantity,
        unit: part.unit,
        time: new Date().toLocaleString('zh-CN')
      }
      this.recentOperations.unshift(operation)
      if (this.recentOperations.length > 10) {
        this.recentOperations.pop()
      }
    },

    resetForm(type) {
      if (type === 'in') {
        this.stockInForm = { partId: '', quantity: 1 }
        this.selectedStockInPart = null
      } else {
        this.stockOutForm = { partId: '', quantity: 1 }
        this.selectedStockOutPart = null
      }
      this.$refs[type === 'in' ? 'stockInFormRef' : 'stockOutFormRef'].resetFields()
    }
  }
}
</script>

<style scoped>
.operation-card {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
}

.part-info {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.part-info p {
  margin: 8px 0;
  font-size: 14px;
}

.stock-quantity {
  font-size: 16px;
  font-weight: bold;
  color: #409EFF;
}

.recent-operations {
  margin-top: 30px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
  color: #303133;
}

.success-text {
  color: #67c23a;
  font-weight: bold;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
