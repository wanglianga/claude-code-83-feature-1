<template>
  <div v-loading="loading">
    <template v-if="order">
      <!-- 概要 -->
      <el-card shadow="never">
        <div class="head">
          <div>
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag style="margin-left: 8px" :type="ORDER_STATUS_TYPES[order.status]">
              {{ ORDER_STATUS_LABELS[order.status] }}
            </el-tag>
            <el-tag v-if="order.hasException" type="danger" style="margin-left: 4px">有异常</el-tag>
            <el-tag v-if="order.infectionRisk" type="danger" effect="dark" style="margin-left: 4px">感染风险</el-tag>
            <el-tag v-if="order.backupKitUsed" type="warning" style="margin-left: 4px">备用服务包</el-tag>
            <el-tag v-if="order.toolIssueCaused" type="warning" style="margin-left: 4px">工具问题改期/取消</el-tag>
            <el-tag style="margin-left: 4px" effect="plain">{{ ORDER_TYPE_LABELS[order.type] }}</el-tag>
          </div>
          <el-space wrap>
            <!-- 理发师动作 -->
            <el-button v-if="canConfirmTools" type="primary" @click="openToolDialog">上门扫码核验</el-button>
            <el-button v-if="canStart" type="primary" @click="doStart">开始服务</el-button>
            <el-button v-if="canComplete" type="success" @click="completeDialog = true">完成服务</el-button>
            <!-- 志愿者动作 -->
            <el-button v-if="canCheckIn" type="warning" @click="checkInDialog = true">志愿者进门核对</el-button>
            <!-- 通用动作 -->
            <el-button v-if="canRate" type="primary" plain @click="rateDialog = true">满意度评价</el-button>
            <el-button v-if="canConfirmPay" type="success" plain @click="doConfirmPay">确认收款</el-button>
            <el-button v-if="!isFinished" type="warning" plain @click="exceptionDialog = true">上报异常</el-button>
            <el-button v-if="canReportInfection" type="danger" plain @click="infectionDialog = true">反馈感染追溯</el-button>
            <el-button v-if="!isFinished" plain @click="rescheduleDialog = true">改约</el-button>
            <el-button v-if="!isFinished" type="danger" plain @click="cancelDialog = true">取消预约</el-button>
          </el-space>
        </div>
        <el-descriptions :column="4" border size="small" style="margin-top: 12px">
          <el-descriptions-item label="老人">{{ order.elderName }}</el-descriptions-item>
          <el-descriptions-item label="上门时间">{{ order.scheduledDate }} {{ order.timeSlot }}</el-descriptions-item>
          <el-descriptions-item label="理发师">{{ order.barberName }}</el-descriptions-item>
          <el-descriptions-item label="志愿者">{{ order.volunteerName || '无需陪同' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ order.address }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag size="small" :type="RISK_TYPES[order.riskLevel]">{{ RISK_LABELS[order.riskLevel] }}</el-tag>
            <el-tag v-if="order.livingAlone" size="small" type="danger" style="margin-left: 4px">独居</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="需家属在场">{{ order.needFamilyPresent ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="费用">总价 ¥{{ order.totalAmount }}，补贴 ¥{{ order.subsidyAmount }}，自费 ¥{{ order.selfPayAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag size="small" :type="PAYMENT_TYPES[order.paymentStatus]">{{ PAYMENT_LABELS[order.paymentStatus] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="满意度" :span="2">
            <template v-if="order.satisfactionRating">
              <el-rate :model-value="order.satisfactionRating" disabled size="small" />
              {{ order.satisfactionComment }}
            </template>
            <span v-else>未评价</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="14">
          <!-- 服务过程记录 -->
          <el-card shadow="never">
            <template #header>服务过程档案</template>
            <el-collapse v-model="activePanels">
              <el-collapse-item title="① 理发师上门前扫码核验（封签/消毒/外观/感染风险）" name="tool">
                <template v-if="detail.toolConfirmation">
                  <el-alert v-if="!detail.toolConfirmation.passed" type="error" :closable="false" show-icon
                    title="扫码核验未通过，禁止开始服务"
                    description="请联系社区改约或启用备用服务包；因工具问题取消/改期不算老人违约，不扣补贴。"
                    style="margin-bottom: 10px" />
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="服务包">
                      {{ detail.toolConfirmation.backupKitUsed ? '备用服务包' : '常规工具包' }}
                      <span class="muted">（#{{ detail.toolConfirmation.kitId }}）</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="封签编号">
                      <el-tag size="small" :type="detail.toolConfirmation.sealIntact ? 'success' : 'danger'">
                        {{ detail.toolConfirmation.sealNo || '无' }} {{ detail.toolConfirmation.sealIntact ? '完好' : '破损' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒日期">{{ detail.toolConfirmation.disinfectedAt || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="消毒有效期">
                      <el-tag size="small" :type="detail.toolConfirmation.disinfectionValid ? 'success' : 'danger'">
                        {{ detail.toolConfirmation.disinfectionValid ? '48小时内有效' : '已超有效期' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒方式">{{ detail.toolConfirmation.disinfectionMethod || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="消毒柜编号">{{ detail.toolConfirmation.cabinetNo || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="责任人">{{ detail.toolConfirmation.responsiblePerson || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="毛巾围布">
                      <el-tag size="small" :type="detail.toolConfirmation.towelDry ? 'success' : 'danger'">
                        {{ detail.toolConfirmation.towelDry ? '干燥' : '受潮' }}
                      </el-tag>
                    </el-descriptions-item>
                  </el-descriptions>
                  <el-space wrap style="margin-top: 8px">
                    <el-tag :type="tagType(detail.toolConfirmation.toolsOk)">理发工具</el-tag>
                    <el-tag :type="tagType(detail.toolConfirmation.capeOk)">围布</el-tag>
                    <el-tag :type="tagType(detail.toolConfirmation.disinfectantOk)">消毒用品</el-tag>
                    <el-tag :type="tagType(detail.toolConfirmation.packOk)">服务包</el-tag>
                    <el-tag :type="tagType(detail.toolConfirmation.toolClean)">刀剪无污渍</el-tag>
                  </el-space>
                  <p v-if="detail.toolConfirmation.missingItems" class="warn-text">
                    遗漏：{{ detail.toolConfirmation.missingItems }}
                  </p>
                  <el-divider content-position="left" style="margin: 10px 0">感染风险与用后处理</el-divider>
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="感染风险">
                      <el-tag size="small" :type="detail.toolConfirmation.infectionRisk === 'NONE' ? 'info' : 'danger'">
                        {{ INFECTION_RISK_LABELS[detail.toolConfirmation.infectionRisk] }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="一次性用品">
                      {{ detail.toolConfirmation.disposableUsed ? '已使用' : '未使用' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="工具单独分装">
                      {{ detail.toolConfirmation.separatelyPacked ? '是' : '否' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="用后处理已记录">
                      {{ detail.toolConfirmation.postHandlingRecorded ? '是' : '否' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="风险说明" :span="2">
                      {{ detail.toolConfirmation.infectionNote || '—' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="用后处理方式" :span="2">
                      {{ detail.toolConfirmation.postHandling || '—' }}
                    </el-descriptions-item>
                  </el-descriptions>
                  <p class="time" style="margin-top: 6px">核验时间：{{ detail.toolConfirmation.confirmedAt }}</p>
                </template>
                <el-empty v-else description="待理发师上门扫码核验" :image-size="40" />

                <!-- 核验不通过的处置入口 -->
                <div v-if="canHandleToolIssue" style="margin-top: 10px">
                  <el-space wrap>
                    <el-button size="small" type="warning" @click="issueDialog = true">联系社区改约</el-button>
                    <el-button size="small" type="primary" @click="openBackup">启用备用服务包</el-button>
                    <el-button size="small" type="danger" plain @click="issueCancelDialog = true">无法服务取消</el-button>
                  </el-space>
                </div>

                <!-- 工具问题处置记录 -->
                <el-table v-if="detail.toolIssues?.length" :data="detail.toolIssues" size="small" style="margin-top: 10px">
                  <el-table-column label="问题" width="130">
                    <template #default="{ row }">{{ TOOL_ISSUE_LABELS[row.issueType] }}</template>
                  </el-table-column>
                  <el-table-column label="处置" width="120">
                    <template #default="{ row }">{{ TOOL_ACTION_LABELS[row.action] }}</template>
                  </el-table-column>
                  <el-table-column label="空跑补偿" width="130">
                    <template #default="{ row }">
                      <el-tag size="small" :type="COMPENSATION_TYPES[row.compensationStatus]">
                        {{ COMPENSATION_LABELS[row.compensationStatus] }}
                      </el-tag>
                      <span v-if="Number(row.compensationAmount) > 0"> ¥{{ row.compensationAmount }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="compensationNote" label="认定说明" min-width="120" show-overflow-tooltip />
                </el-table>
              </el-collapse-item>
              <el-collapse-item title="② 志愿者陪同记录（进门安全 / 老人状态 / 家属授权）" name="visit">
                <template v-if="detail.visitRecord">
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="进门安全">
                      <el-tag size="small" :type="tagType(detail.visitRecord.entrySafe)">
                        {{ detail.visitRecord.entrySafe ? '正常' : '异常' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="精神状态">{{ detail.visitRecord.mentalState }}</el-descriptions-item>
                    <el-descriptions-item label="老人状态" :span="2">{{ detail.visitRecord.elderState }}</el-descriptions-item>
                    <el-descriptions-item label="家属授权">
                      <el-tag size="small" :type="tagType(detail.visitRecord.familyAuthorized)">
                        {{ detail.visitRecord.familyAuthorized ? '已确认' : '未确认' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="备注">{{ detail.visitRecord.notes || '—' }}</el-descriptions-item>
                  </el-descriptions>
                  <photo-view :photos="detail.visitRecord.photos" />
                </template>
                <el-empty v-else description="待志愿者进门核对" :image-size="40" />
              </el-collapse-item>
              <el-collapse-item title="③ 服务完成档案（剪发照片 / 金额）" name="record">
                <template v-if="detail.serviceRecord">
                  <p>补贴 ¥{{ detail.serviceRecord.subsidyAmount }}，自费 ¥{{ detail.serviceRecord.selfPayAmount }}；
                    {{ detail.serviceRecord.paymentNote }}</p>
                  <photo-view :photos="detail.serviceRecord.haircutPhotos" />
                </template>
                <el-empty v-else description="服务未完成" :image-size="40" />
              </el-collapse-item>
            </el-collapse>
          </el-card>

          <!-- 异常 -->
          <el-card shadow="never" style="margin-top: 16px">
            <template #header>异常记录（老人/家属/理发师/志愿者/社区/财务协同处理）</template>
            <el-table :data="exceptions" size="small" empty-text="无异常">
              <el-table-column label="类型" width="110">
                <template #default="{ row }">{{ EXCEPTION_TYPE_LABELS[row.type] }}</template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="EXCEPTION_STATUS_TYPES[row.status]">
                    {{ EXCEPTION_STATUS_LABELS[row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="财务介入" width="80">
                <template #default="{ row }">{{ row.financeInvolved ? '是' : '—' }}</template>
              </el-table-column>
              <el-table-column prop="resolution" label="处理结果" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.resolution || '—' }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="10">
          <!-- 时间线 -->
          <el-card shadow="never">
            <template #header>服务单时间线</template>
            <el-timeline style="padding-left: 4px">
              <el-timeline-item v-for="e in detail.events" :key="e.id" :timestamp="e.createdAt" placement="top">
                <div>
                  <el-tag size="small" effect="plain">{{ EVENT_TYPE_LABELS[e.eventType] || e.eventType }}</el-tag>
                  <span class="actor">{{ e.actorName || '系统' }}（{{ ROLE_LABELS[e.actorRole] || e.actorRole }}）</span>
                </div>
                <div class="event-content">{{ e.content }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-card>

          <!-- 补贴与回访 -->
          <el-card shadow="never" style="margin-top: 16px" v-if="detail.subsidies?.length || detail.followUps?.length">
            <template #header>补贴与回访</template>
            <div v-for="s in detail.subsidies" :key="'s' + s.id" class="line">
              公益补贴 ¥{{ s.amount }}
              <el-tag size="small" :type="SUBSIDY_STATUS_TYPES[s.status]">{{ SUBSIDY_STATUS_LABELS[s.status] }}</el-tag>
              <span v-if="s.note" class="muted">（{{ s.note }}）</span>
            </div>
            <div v-for="f in detail.followUps" :key="'f' + f.id" class="line">
              [{{ FOLLOWUP_TYPE_LABELS[f.type] }}] {{ f.content }}
              <el-tag size="small" :type="f.status === 'DONE' ? 'success' : 'warning'">
                {{ FOLLOWUP_STATUS_LABELS[f.status] }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 上门扫码核验对话框 -->
    <el-dialog v-model="toolDialog" title="上门前扫码核验服务包" width="640px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
        title="扫码核验封签、消毒日期、消毒方式、消毒柜编号和责任人；任一硬性项不达标将无法开始服务。" />
      <el-descriptions :column="2" size="small" border v-if="kitSnapshot">
        <el-descriptions-item label="服务包">{{ kitSnapshot.name }}</el-descriptions-item>
        <el-descriptions-item label="封签编号">{{ kitSnapshot.sealNo || '未登记' }}</el-descriptions-item>
        <el-descriptions-item label="消毒日期">{{ kitSnapshot.disinfectedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="消毒方式">{{ kitSnapshot.disinfectionMethod || '—' }}</el-descriptions-item>
        <el-descriptions-item label="消毒柜编号">{{ kitSnapshot.cabinetNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="责任人">{{ kitSnapshot.responsiblePerson || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="120px" style="margin-top: 12px">
        <el-divider content-position="left" style="margin: 6px 0">封签与外观检查</el-divider>
        <el-form-item label="封签完好">
          <el-switch v-model="toolForm.sealIntact" active-text="完好" inactive-text="破损" />
        </el-form-item>
        <el-form-item label="消毒在有效期">
          <el-tag size="small" :type="kitDisinfectionValid ? 'success' : 'danger'">
            {{ kitDisinfectionValid ? '48小时内有效' : '已超有效期' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="毛巾围布干燥">
          <el-switch v-model="toolForm.towelDry" active-text="干燥" inactive-text="受潮" />
        </el-form-item>
        <el-form-item label="刀剪无污渍">
          <el-switch v-model="toolForm.toolClean" active-text="干净" inactive-text="有污渍" />
        </el-form-item>
        <el-divider content-position="left" style="margin: 6px 0">工具齐备</el-divider>
        <el-form-item label="理发工具"><el-switch v-model="toolForm.toolsOk" /></el-form-item>
        <el-form-item label="围布"><el-switch v-model="toolForm.capeOk" /></el-form-item>
        <el-form-item label="消毒用品"><el-switch v-model="toolForm.disinfectantOk" /></el-form-item>
        <el-form-item label="服务包"><el-switch v-model="toolForm.packOk" /></el-form-item>
        <el-form-item v-if="!allItemsOk" label="遗漏说明">
          <el-input v-model="toolForm.missingItems" placeholder="遗漏物品" />
        </el-form-item>
        <el-divider content-position="left" style="margin: 6px 0">老人感染风险</el-divider>
        <el-form-item label="感染风险">
          <el-select v-model="toolForm.infectionRisk" style="width: 100%">
            <el-option v-for="(v, k) in INFECTION_RISK_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <template v-if="toolForm.infectionRisk !== 'NONE'">
          <el-alert type="warning" :closable="false" show-icon
            title="服务单将标记感染风险：工具必须单独分装，并记录用后处理" style="margin-bottom: 10px" />
          <el-form-item label="风险情况"><el-input v-model="toolForm.infectionNote" placeholder="如：头顶头癣/手臂伤口" /></el-form-item>
          <el-form-item label="一次性用品"><el-switch v-model="toolForm.disposableUsed" active-text="使用一次性用品" /></el-form-item>
          <el-form-item label="工具单独分装"><el-switch v-model="toolForm.separatelyPacked" active-text="已单独分装" /></el-form-item>
          <el-form-item label="用后处理方式">
            <el-input v-model="toolForm.postHandling" type="textarea" :rows="2"
              placeholder="如：单独封装带回，含氯消毒剂浸泡30分钟后高温蒸煮；一次性用品按医废处理" />
          </el-form-item>
        </template>
      </el-form>
      <el-alert v-if="!formPassHintOk" type="error" :closable="false" show-icon
        title="当前核验项未全部达标，提交后服务单不会进入可服务状态，请改约或启用备用服务包。" style="margin-top: 4px" />
      <template #footer>
        <el-button @click="toolDialog = false">取消</el-button>
        <el-button type="primary" @click="doConfirmTools">提交扫码核验</el-button>
      </template>
    </el-dialog>

    <!-- 工具问题：改约 / 取消 对话框 -->
    <el-dialog v-model="issueDialog" title="工具问题处置：联系社区改约" width="440px">
      <el-form label-width="90px">
        <el-form-item label="问题类型">
          <el-select v-model="issueForm.issueType" style="width: 100%">
            <el-option v-for="(v, k) in TOOL_ISSUE_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题详情"><el-input v-model="issueForm.issueDetail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="改约日期">
          <el-date-picker v-model="issueForm.rescheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="改约时段">
          <el-select v-model="issueForm.timeSlot" style="width: 100%">
            <el-option v-for="s in TIME_SLOTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="success" :closable="false" :title="'因工具问题改期不算老人违约，不扣补贴；空跑补偿由社区认定。'" />
      <template #footer>
        <el-button @click="issueDialog = false">取消</el-button>
        <el-button type="warning" @click="doIssue('RESCHEDULE')">提交并改约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueCancelDialog" title="工具问题处置：无法服务取消" width="440px">
      <el-form label-width="90px">
        <el-form-item label="问题类型">
          <el-select v-model="issueForm.issueType" style="width: 100%">
            <el-option v-for="(v, k) in TOOL_ISSUE_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题详情"><el-input v-model="issueForm.issueDetail" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-alert type="success" :closable="false" title="因工具问题取消不算老人违约、不扣补贴；工具遗漏或消毒失责空跑由理发师承担。" />
      <template #footer>
        <el-button @click="issueCancelDialog = false">返回</el-button>
        <el-button type="danger" @click="doIssue('CANCEL')">确认取消</el-button>
      </template>
    </el-dialog>

    <!-- 启用备用服务包对话框 -->
    <el-dialog v-model="backupDialog" title="启用备用服务包" width="560px">
      <el-table :data="backupKits" size="small" @row-click="pickBackup" highlight-current-row>
        <el-table-column prop="name" label="备用包" min-width="130" />
        <el-table-column prop="sealNo" label="封签编号" width="140" />
        <el-table-column label="消毒有效期" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="backupValid(row) ? 'success' : 'danger'">
              {{ backupValid(row) ? '有效' : '失效/待补录' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsiblePerson" label="责任人" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!backupValid(row)" @click.stop="confirmBackup(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="muted" style="margin-top: 8px">启用后请重新扫码核验；备用包使用后社区须补录消毒记录，未补录前不得再次派单。</p>
      <template #footer>
        <el-button @click="backupDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 交叉感染反馈对话框 -->
    <el-dialog v-model="infectionDialog" title="反馈皮肤不适 / 登记交叉感染追溯" width="500px">
      <el-form label-width="100px">
        <el-form-item label="症状">
          <el-select v-model="infectionForm.symptom" style="width: 100%">
            <el-option v-for="s in ['皮肤瘙痒', '红疹', '皮疹', '确诊感染', '其他不适']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="涉及工具编号"><el-input v-model="infectionForm.toolItemNo" placeholder="如剪刀/剃刀单件编号（可选）" /></el-form-item>
        <el-form-item label="情况说明"><el-input v-model="infectionForm.description" type="textarea" :rows="3"
          placeholder="服务后出现时间、就医情况等；社区将按理发师、服务包、工具编号和同日记录反查" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infectionDialog = false">取消</el-button>
        <el-button type="danger" @click="doReportInfection">提交并发起反查</el-button>
      </template>
    </el-dialog>

    <!-- 志愿者进门核对对话框 -->
    <el-dialog v-model="checkInDialog" title="志愿者陪同进门核对" width="520px">
      <el-form label-width="100px">
        <el-form-item label="进门安全"><el-switch v-model="checkInForm.entrySafe" active-text="正常" inactive-text="异常" /></el-form-item>
        <el-form-item label="老人状态"><el-input v-model="checkInForm.elderState" placeholder="核对老人身体状态" /></el-form-item>
        <el-form-item label="精神状态">
          <el-select v-model="checkInForm.mentalState">
            <el-option v-for="m in ['良好', '一般', '萎靡', '异常']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="家属授权">
          <el-switch v-model="checkInForm.familyAuthorized" active-text="已确认" inactive-text="未确认" />
          <span v-if="order?.needFamilyPresent" class="warn-text" style="margin-left: 8px">该老人要求家属在场</span>
        </el-form-item>
        <el-form-item label="现场照片"><photo-upload v-model="checkInForm.photos" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="checkInForm.notes" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkInDialog = false">取消</el-button>
        <el-button type="primary" @click="doCheckIn">提交核对</el-button>
      </template>
    </el-dialog>

    <!-- 完成服务对话框 -->
    <el-dialog v-model="completeDialog" title="完成服务" width="520px">
      <el-form label-width="100px">
        <el-form-item label="剪发照片"><photo-upload v-model="completeForm.photos" /></el-form-item>
        <el-form-item label="收款备注"><el-input v-model="completeForm.paymentNote" placeholder="如：自费部分已现金收讫" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialog = false">取消</el-button>
        <el-button type="success" @click="doComplete">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- 评价对话框 -->
    <el-dialog v-model="rateDialog" title="满意度评价" width="420px">
      <el-rate v-model="rateForm.rating" :max="5" style="margin-bottom: 12px" />
      <el-input v-model="rateForm.comment" type="textarea" :rows="3" placeholder="评价内容（可选）" />
      <template #footer>
        <el-button @click="rateDialog = false">取消</el-button>
        <el-button type="primary" @click="doRate">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- 异常上报对话框 -->
    <el-dialog v-model="exceptionDialog" title="上报异常" width="480px">
      <el-form label-width="90px">
        <el-form-item label="异常类型">
          <el-select v-model="exceptionForm.type" style="width: 100%">
            <el-option v-for="(v, k) in EXCEPTION_TYPE_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="情况描述">
          <el-input v-model="exceptionForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exceptionDialog = false">取消</el-button>
        <el-button type="danger" @click="doReportException">提交</el-button>
      </template>
    </el-dialog>

    <!-- 改约对话框 -->
    <el-dialog v-model="rescheduleDialog" title="改约" width="420px">
      <el-form label-width="90px">
        <el-form-item label="新日期">
          <el-date-picker v-model="rescheduleForm.newDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="新时段">
          <el-select v-model="rescheduleForm.newTimeSlot" style="width: 100%">
            <el-option v-for="s in TIME_SLOTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="改约原因"><el-input v-model="rescheduleForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rescheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="doReschedule">确认改约</el-button>
      </template>
    </el-dialog>

    <!-- 取消对话框 -->
    <el-dialog v-model="cancelDialog" title="取消预约" width="420px">
      <el-input v-model="cancelReason" placeholder="取消原因" />
      <template #footer>
        <el-button @click="cancelDialog = false">返回</el-button>
        <el-button type="danger" @click="doCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  orderDetail, confirmTools, checkIn, startService, completeOrder, cancelOrder,
  rescheduleOrder, rateOrder, confirmPayment, reportException, listOrderExceptions,
  getToolKit, listBackupKits, handleToolIssue, reportInfection
} from '../api'
import { useUserStore } from '../store/user'
import PhotoUpload from '../components/PhotoUpload.vue'
import PhotoView from '../components/PhotoView.vue'
import dayjs from 'dayjs'
import {
  ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, ORDER_TYPE_LABELS, RISK_LABELS, RISK_TYPES,
  PAYMENT_LABELS, PAYMENT_TYPES, EXCEPTION_TYPE_LABELS, EXCEPTION_STATUS_LABELS,
  EXCEPTION_STATUS_TYPES, SUBSIDY_STATUS_LABELS, SUBSIDY_STATUS_TYPES,
  FOLLOWUP_TYPE_LABELS, FOLLOWUP_STATUS_LABELS, ROLE_LABELS, EVENT_TYPE_LABELS, TIME_SLOTS,
  INFECTION_RISK_LABELS, TOOL_ISSUE_LABELS, TOOL_ACTION_LABELS, COMPENSATION_LABELS, COMPENSATION_TYPES
} from '../utils/labels'

const route = useRoute()
const store = useUserStore()
const id = route.params.id

const loading = ref(false)
const detail = ref({})
const exceptions = ref([])
const activePanels = ref(['tool', 'visit', 'record'])

const toolDialog = ref(false)
const checkInDialog = ref(false)
const completeDialog = ref(false)
const rateDialog = ref(false)
const exceptionDialog = ref(false)
const rescheduleDialog = ref(false)
const cancelDialog = ref(false)
const issueDialog = ref(false)
const issueCancelDialog = ref(false)
const backupDialog = ref(false)
const infectionDialog = ref(false)
const cancelReason = ref('')

const defaultToolForm = () => ({
  kitId: null, sealIntact: true, towelDry: true, toolClean: true,
  toolsOk: true, capeOk: true, disinfectantOk: true, packOk: true, missingItems: '',
  infectionRisk: 'NONE', infectionNote: '', separatelyPacked: false, disposableUsed: false, postHandling: ''
})
const toolForm = reactive(defaultToolForm())
const kitSnapshot = ref(null)
const backupKits = ref([])
const issueForm = reactive({ issueType: 'DISINFECTION_EXPIRED', issueDetail: '', rescheduleDate: '', timeSlot: '09:00-11:00' })
const infectionForm = reactive({ symptom: '红疹', toolItemNo: '', description: '' })
const checkInForm = reactive({ entrySafe: true, elderState: '', mentalState: '良好', familyAuthorized: false, photos: [], notes: '' })
const completeForm = reactive({ photos: [], paymentNote: '' })
const rateForm = reactive({ rating: 5, comment: '' })
const exceptionForm = reactive({ type: 'ELDER_UNWELL', description: '' })
const rescheduleForm = reactive({ newDate: '', newTimeSlot: '09:00-11:00', reason: '' })

const order = computed(() => detail.value.order)
const isFinished = computed(() => ['COMPLETED', 'CANCELLED'].includes(order.value?.status))
const isBarberOfOrder = computed(() =>
  store.role === 'BARBER' && order.value?.barberId === store.user?.id)
const isVolunteerOfOrder = computed(() =>
  store.role === 'VOLUNTEER' && order.value?.volunteerId === store.user?.id)
const staffLike = computed(() => ['ADMIN', 'STAFF'].includes(store.role))

const canConfirmTools = computed(() =>
  (isBarberOfOrder.value || staffLike.value) && order.value?.status === 'ASSIGNED')
const canCheckIn = computed(() =>
  (isVolunteerOfOrder.value || staffLike.value) && order.value?.volunteerId
  && ['ASSIGNED', 'TOOL_CONFIRMED'].includes(order.value?.status))
const canStart = computed(() =>
  (isBarberOfOrder.value || staffLike.value)
  && (order.value?.status === 'ON_SITE' || (order.value?.status === 'TOOL_CONFIRMED' && !order.value?.volunteerId)))
const canComplete = computed(() =>
  (isBarberOfOrder.value || staffLike.value) && order.value?.status === 'IN_SERVICE')
const canRate = computed(() =>
  ['FAMILY', 'STAFF', 'ADMIN'].includes(store.role) && order.value?.status === 'COMPLETED' && !order.value?.satisfactionRating)
const canConfirmPay = computed(() =>
  ['BARBER', 'STAFF', 'ADMIN', 'FINANCE'].includes(store.role)
  && order.value?.status === 'COMPLETED'
  && ['UNPAID', 'DISPUTED'].includes(order.value?.paymentStatus)
  && Number(order.value?.selfPayAmount) > 0)
const canReportInfection = computed(() =>
  ['FAMILY', 'STAFF', 'ADMIN', 'BARBER', 'VOLUNTEER'].includes(store.role)
  && order.value?.status === 'COMPLETED')
// 核验未通过（或尚未核验）时，理发师/社区可发起改约、启用备用包或取消
const canHandleToolIssue = computed(() =>
  (isBarberOfOrder.value || staffLike.value)
  && ['ASSIGNED'].includes(order.value?.status))

const allItemsOk = computed(() =>
  toolForm.toolsOk && toolForm.capeOk && toolForm.disinfectantOk && toolForm.packOk)
const kitDisinfectionValid = computed(() => {
  if (!kitSnapshot.value?.disinfectedAt) return false
  return dayjs(kitSnapshot.value.disinfectedAt).add(48, 'hour').isAfter(dayjs())
})
const formPassHintOk = computed(() =>
  kitDisinfectionValid.value && toolForm.sealIntact && toolForm.towelDry && toolForm.toolClean && allItemsOk.value
  && (toolForm.infectionRisk === 'NONE' || (toolForm.separatelyPacked && toolForm.postHandling)))

const backupValid = (row) =>
  !row.disinfectionPending && row.disinfectedAt && dayjs(row.disinfectedAt).add(48, 'hour').isAfter(dayjs())

const tagType = (ok) => (ok ? 'success' : 'danger')

const load = async () => {
  loading.value = true
  try {
    detail.value = await orderDetail(id)
    exceptions.value = await listOrderExceptions(id)
  } finally {
    loading.value = false
  }
}

const openToolDialog = async () => {
  Object.assign(toolForm, defaultToolForm())
  kitSnapshot.value = null
  try {
    kitSnapshot.value = await getToolKit(order.value.barberId)
    toolForm.kitId = kitSnapshot.value?.id ?? null
  } catch (e) {
    // 无工具包时仍可提交核验（后端会给出提示）
  }
  toolDialog.value = true
}

const doConfirmTools = async () => {
  const res = await confirmTools(id, { ...toolForm })
  if (res.passed) {
    ElMessage.success('扫码核验通过，可以进门服务')
  } else {
    ElMessage.error('核验未通过，已禁止开始服务，请改约或启用备用服务包')
  }
  toolDialog.value = false
  load()
}

const openBackup = async () => {
  backupKits.value = await listBackupKits()
  backupDialog.value = true
}
const pickBackup = () => {}
const confirmBackup = async (row) => {
  await ElMessageBox.confirm(`确认启用备用服务包「${row.name}」？启用后需重新扫码核验。`, '启用备用包', { type: 'warning' })
  await handleToolIssue({
    orderId: Number(id), issueType: issueForm.issueType, issueDetail: '启用备用服务包应急',
    action: 'BACKUP', backupKitId: row.id
  })
  ElMessage.success('已启用备用服务包，请重新扫码核验')
  backupDialog.value = false
  load()
}

const doIssue = async (action) => {
  if (action === 'RESCHEDULE' && !issueForm.rescheduleDate) {
    ElMessage.warning('请选择改约日期')
    return
  }
  await handleToolIssue({ orderId: Number(id), ...issueForm, action })
  ElMessage.success(action === 'CANCEL' ? '已按工具问题取消（不计老人违约）' : '已联系社区改约（不计老人违约）')
  issueDialog.value = false
  issueCancelDialog.value = false
  load()
}

const doReportInfection = async () => {
  await reportInfection({ orderId: Number(id), ...infectionForm })
  ElMessage.success('已登记并发起同工具/同日服务反查，社区将通知家属和志愿者')
  infectionDialog.value = false
  load()
}

const doCheckIn = async () => {
  await checkIn(id, { ...checkInForm, photos: JSON.stringify(checkInForm.photos) })
  ElMessage.success('进门核对完成')
  checkInDialog.value = false
  load()
}

const doStart = async () => {
  await startService(id)
  ElMessage.success('已开始服务')
  load()
}

const doComplete = async () => {
  await completeOrder(id, { haircutPhotos: JSON.stringify(completeForm.photos), paymentNote: completeForm.paymentNote })
  ElMessage.success('服务已完成，档案已生成')
  completeDialog.value = false
  load()
}

const doRate = async () => {
  await rateOrder(id, rateForm)
  ElMessage.success('评价成功')
  rateDialog.value = false
  load()
}

const doConfirmPay = async () => {
  await confirmPayment(id)
  ElMessage.success('已确认收款')
  load()
}

const doReportException = async () => {
  await reportException({ orderId: Number(id), ...exceptionForm })
  ElMessage.success('异常已上报，社区与相关方已收到通知')
  exceptionDialog.value = false
  load()
}

const doReschedule = async () => {
  if (!rescheduleForm.newDate) {
    ElMessage.warning('请选择新日期')
    return
  }
  await rescheduleOrder(id, rescheduleForm)
  ElMessage.success('改约成功')
  rescheduleDialog.value = false
  load()
}

const doCancel = async () => {
  if (!cancelReason.value) {
    ElMessage.warning('请填写取消原因')
    return
  }
  await ElMessageBox.confirm('确认取消该预约吗？', '提示', { type: 'warning' })
  await cancelOrder(id, { reason: cancelReason.value })
  ElMessage.success('已取消')
  cancelDialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.order-no {
  font-size: 18px;
  font-weight: 700;
}
.actor {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.event-content {
  margin-top: 4px;
  color: #606266;
}
.warn-text {
  color: #e6a23c;
}
.time {
  color: #909399;
  font-size: 12px;
}
.line {
  margin-bottom: 8px;
}
.muted {
  color: #909399;
  font-size: 12px;
}
</style>
