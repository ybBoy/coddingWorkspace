<template>
  <div class="file-upload">
    <el-card>
      <div slot="header" class="card-header">
        <span>文件上传</span>
      </div>
      
      <el-upload
        class="upload-demo"
        drag
        action="/api/upload"
        :auto-upload="false"
        :on-change="handleChange"
        :on-success="handleSuccess"
        :on-error="handleError"
        :limit="1"
        :file-list="fileList"
        accept=".xlsx,.xls,.csv"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持上传 .xlsx、.xls、.csv 格式的文件
        </div>
        <div class="el-upload__tip">
          文件格式要求：交易时间(yyyyMMdd HH:mm:ss)、成交价格、成交数量、买入账户、卖出账户
        </div>
      </el-upload>
      
      <div style="margin-top: 20px;">
        <el-button 
          type="primary" 
          :disabled="fileList.length === 0 || uploading"
          @click="submitUpload"
          :loading="uploading"
        >
          上传并解析
        </el-button>
      </div>
      
      <el-alert
        v-if="uploadMessage"
        :title="uploadMessage"
        :type="uploadMessageType"
        style="margin-top: 20px;"
        :closable="false"
      />
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'FileUpload',
  data() {
    return {
      fileList: [],
      uploading: false,
      uploadMessage: '',
      uploadMessageType: 'success'
    }
  },
  methods: {
    handleChange(file) {
      this.fileList = [file]
      this.uploadMessage = ''
    },
    submitUpload() {
      if (this.fileList.length === 0) {
        this.$message.warning('请先选择文件')
        return
      }
      
      this.uploading = true
      const formData = new FormData()
      formData.append('file', this.fileList[0].raw)
      
      this.$axios.post('/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(response => {
          if (response.data.success) {
            this.uploadMessage = response.data.message
            this.uploadMessageType = 'success'
            this.$emit('file-uploaded')
          } else {
            this.uploadMessage = response.data.message
            this.uploadMessageType = 'error'
          }
        })
        .catch(error => {
          console.error('上传错误:', error)
          this.uploadMessage = '上传失败，请检查网络连接'
          this.uploadMessageType = 'error'
        })
        .finally(() => {
          this.uploading = false
        })
    },
    handleSuccess(response) {
      if (response.success) {
        this.uploadMessage = response.message
        this.uploadMessageType = 'success'
        this.$emit('file-uploaded')
      } else {
        this.uploadMessage = response.message
        this.uploadMessageType = 'error'
      }
    },
    handleError(error) {
      console.error('上传错误:', error)
      this.uploadMessage = '上传失败，请检查网络连接'
      this.uploadMessageType = 'error'
    }
  }
}
</script>

<style scoped>
.card-header {
  font-size: 18px;
  font-weight: bold;
}

.upload-demo {
  margin-top: 10px;
}

.el-upload__text em {
  color: #409EFF;
}
</style>
