<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-s-claim" style="margin-right: 10px;"></i>提交退货入库申请
    </div>
    
    <el-card class="form-card">
      <el-form :model="requestForm" :rules="rules" ref="requestFormRef" label-width="100px">
        <el-form-item label="申请明细">
          <el-table :data="requestForm.items" style="width: 100%" border>
            <el-table-column label="零件" width="300">
              <template slot-scope="scope">
                <el-select
                  v-model="scope.row.partId"
                  filterable
                  placeholder="请选择零件"
                  style="width: 100%"
                  @change="onPartChange(scope.$index, scope.row.partId)"
                >
                  <el-option
                    v-for="part in availableParts"
                    :key="part.id"
                    :label="part.name + ' (' + part.id + ')'"
                    :value="part.id"
                  >
                    <span style="float: left">{{ part.name }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px">
                      编号: {{ part.id }}
                    </span>
                  </el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="归还数量" width="180">
              <template slot-scope="scope">
                <el-input-number
                  v-model="scope.row.quantity"
                  :min="1"
                  :max="99999"
                  :disabled="!scope.row.partId"
                  style="width: 160px"
                ></el-input-number>
              </template>
            </el-table-column>
            <el-table-column label="当前库存" width="120">
              <template slot-scope="scope">
                <span v-if="getPartInfo(scope.row.partId)">
                  {{ getPartInfo(scope.row.partId).quantity }} {{ getPartInfo(scope.row.partId).unit }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="150">
              <template slot-scope="scope">
                <span v-if="getPartInfo(scope.row.partId)">
                  {{ getPartInfo(scope.row.partId).specification }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template slot-scope="scope">
                <el-button
                  type="danger"
                  icon="el-icon-delete"
                  size="small"
                  circle
                  @click="removeItem(scope.$index)"
                  :disabled="requestForm.items.length <= 1"
                ></el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            type="primary"
            icon="el-icon-plus"
            @click="addItem"
            style="margin-top: 15px;"
          >
            添加零件
          </el-button>
        </el-form-item>
        
        <el-form-item label="申请备注" prop="remark">
          <el-input
            v-model="requestForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入退货原因或备注（选填）"
          ></el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submitRequest" :loading="submitting" size="large">
            <i class="el-icon-upload"></i> 提交申请
          </el-button>
          <el-button @click="resetForm" size="large">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <div class="tips-card" v-if="availableParts.length > 0">
      <el-alert
        title="申请说明"
        type="info"
        :closable="false"
      >
        <template slot="default">
          <ul style="margin: 10px 0; padding-left: 20px;">
            <li>请选择需要归还的零件和数量，将使用不完的零件归还入库</li>
            <li>申请提交后，状态为"待审核"，等待管理员审核</li>
            <li>管理员审核通过后，系统会自动完成入库操作，库存将相应增加</li>
            <li>您可以在"我的申请"页面查看申请状态和审核结果</li>
          </ul>
        </template>
      </el-alert>
    </div>
    
    <el-empty v-if="availableParts.length === 0 && !loading" description="暂无可申请的零件" :image-size="120">
      <el-button type="primary" @click="loadParts">刷新</el-button>
    </el-empty>
  </div>
</template>

<script>
import partApi from '../../api/partApi'

export default {
  name: 'UserReturnRequestSubmit',
  data() {
    return {
      loading: false,
      submitting: false,
      availableParts: [],
      requestForm: {
        items: [
          { partId: '', quantity: 1 }
        ],
        remark: ''
      },
      rules: {
        remark: [
          { max: 500, message: '备注不能超过500个字符', trigger: 'blur' }
        ]
      }
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
          this.availableParts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载零件列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    addItem() {
      this.requestForm.items.push({
        partId: '',
        quantity: 1
      })
    },
    
    removeItem(index) {
      this.requestForm.items.splice(index, 1)
    },
    
    onPartChange(index, partId) {
      const part = this.getPartInfo(partId)
      if (part) {
        this.$set(this.requestForm.items[index], 'quantity', 1)
      }
    },
    
    getPartInfo(partId) {
      if (!partId) return null
      return this.availableParts.find(p => p.id === partId)
    },
    
    validateForm() {
      const items = this.requestForm.items
      
      if (items.length === 0) {
        this.$message.warning('请至少添加一个零件')
        return false
      }
      
      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (!item.partId) {
          this.$message.warning('第 ' + (i + 1) + ' 行请选择零件')
          return false
        }
        if (item.quantity < 1) {
          this.$message.warning('第 ' + (i + 1) + ' 行归还数量必须大于0')
          return false
        }
      }
      
      const partIds = items.map(i => i.partId)
      const uniquePartIds = [...new Set(partIds)]
      if (uniquePartIds.length !== partIds.length) {
        this.$message.warning('不能重复选择同一个零件')
        return false
      }
      
      return true
    },
    
    async submitRequest() {
      if (!this.validateForm()) {
        return
      }
      
      const submitData = {
        items: this.requestForm.items.map(item => ({
          partId: item.partId,
          quantity: item.quantity
        })),
        remark: this.requestForm.remark
      }
      
      this.submitting = true
      try {
        const res = await partApi.user.submitReturnRequest(submitData)
        if (res.success) {
          this.$message.success('退货入库申请提交成功！申请编号: ' + res.data.id)
          this.$router.push('/user/requests')
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('提交申请失败: ' + error.message)
      } finally {
        this.submitting = false
      }
    },
    
    resetForm() {
      this.requestForm = {
        items: [
          { partId: '', quantity: 1 }
        ],
        remark: ''
      }
      this.$refs.requestFormRef.resetFields()
    }
  }
}
</script>

<style scoped>
.form-card {
  margin-bottom: 20px;
}

.tips-card {
  margin-top: 20px;
}

::v-deep .el-table .el-form-item {
  margin-bottom: 0;
}
</style>
