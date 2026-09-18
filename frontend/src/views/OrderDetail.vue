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
            <el-tag v-if="order.spareUsed" type="warning" style="margin-left: 4px">已用备用服务包</el-tag>
            <el-tag v-if="order.emptyRun" type="info" style="margin-left: 4px">理发师空跑</el-tag>
            <el-tag style="margin-left: 4px" effect="plain">{{ ORDER_TYPE_LABELS[order.type] }}</el-tag>
          </div>
          <el-space wrap>
            <el-button v-if="canConfirmTools" type="primary" @click="openToolDialog">上门前扫码核验</el-button>
            <el-button v-if="canStart" type="primary" @click="doStart">开始服务</el-button>
            <el-button v-if="canComplete" type="success" @click="completeDialog = true">完成服务</el-button>
            <el-button v-if="canCheckIn" type="warning" @click="checkInDialog = true">志愿者进门核对</el-button>
            <el-button v-if="canReportInfection" type="danger" plain @click="infectionDialog = true">反馈皮肤/感染</el-button>
            <el-button v-if="canRate" type="primary" plain @click="rateDialog = true">满意度评价</el-button>
            <el-button v-if="canConfirmPay" type="success" plain @click="doConfirmPay">确认收款</el-button>
            <el-button v-if="!isFinished" type="warning" plain @click="exceptionDialog = true">上报异常</el-button>
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
          <el-descriptions-item label="感染风险" :span="2">
            <template v-if="order.infectionRisk">
              <el-tag size="small" type="danger">已标记</el-tag>
              <span class="muted">{{ order.infectionRiskReason }}</span>
            </template>
            <span v-else>无</span>
          </el-descriptions-item>
          <el-descriptions-item label="实际使用工具包" :span="2">
            {{ order.usedKitName || '—' }}
            <el-tag v-if="order.usedKitCompliant === false" size="small" type="danger" style="margin-left: 4px">未达标</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用后处理" :span="2">{{ order.postUseHandling || '—' }}</el-descriptions-item>
          <el-descriptions-item label="费用">总价 ¥{{ order.totalAmount }}，补贴 ¥{{ order.subsidyAmount }}，自费 ¥{{ order.selfPayAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag size="small" :type="PAYMENT_TYPES[order.paymentStatus]">{{ PAYMENT_LABELS[order.paymentStatus] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="空跑补偿" :span="2">
            <template v-if="order.compensationStatus">
              <el-tag size="small" :type="COMPENSATION_STATUS_TYPES[order.compensationStatus]">
                {{ COMPENSATION_STATUS_LABELS[order.compensationStatus] }}
              </el-tag>
              <span v-if="order.compensationAmount" class="muted"> ¥{{ order.compensationAmount }}</span>
              <el-button v-if="canAdjudicate" link type="primary" @click="compDialog = true">认定补偿</el-button>
              <div v-if="order.compensationNote" class="muted">{{ order.compensationNote }}</div>
            </template>
            <span v-else>—</span>
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
              <el-collapse-item title="① 理发师上门前扫码核验（封签/消毒日期/方式/消毒柜/责任人）" name="tool">
                <template v-if="detail.toolConfirmation">
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="封签编号">{{ detail.toolConfirmation.sealCode || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="封签完好">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.sealIntact)">{{ detail.toolConfirmation.sealIntact ? '完好' : '破损' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒在有效期内">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.disinfectionValid)">{{ detail.toolConfirmation.disinfectionValid ? '有效' : '过期' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒方式合规">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.methodOk)">{{ detail.toolConfirmation.methodOk ? '可查' : '缺失' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒柜编号">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.cabinetOk)">{{ detail.toolConfirmation.cabinetOk ? '可查' : '缺失' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒责任人">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.responsibleOk)">{{ detail.toolConfirmation.responsibleOk ? '可查' : '缺失' }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="工具齐备">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.toolsOk)">齐备</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="剪刀/剃刀清洁">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.toolsClean)">无污渍</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="围布齐备">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.capeOk)">齐备</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="毛巾/围布干燥">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.clothDry)">干燥</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="消毒用品">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.disinfectantOk)">齐备</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="服务包">
                      <el-tag size="small" :type="tagType(detail.toolConfirmation.packOk)">齐备</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="感染风险服务" :span="2">
                      <el-tag size="small" :type="detail.toolConfirmation.infectionRisk ? 'danger' : 'info'">
                        {{ detail.toolConfirmation.infectionRisk ? '是：' + detail.toolConfirmation.infectionRiskReason : '否' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="工具单独分装">
                      <el-tag size="small" :type="tagType(!detail.toolConfirmation.infectionRisk || detail.toolConfirmation.toolsSeparated)">
                        {{ detail.toolConfirmation.toolsSeparated ? '已单独分装' : '未分装' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="一次性用品">
                      <el-tag size="small" :type="detail.toolConfirmation.disposableUsed ? 'warning' : 'info'">
                        {{ detail.toolConfirmation.disposableUsed ? '已使用' : '未使用' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="用后处理" :span="2">{{ detail.toolConfirmation.postUseHandling || '—' }}</el-descriptions-item>
                  </el-descriptions>
                  <p v-if="detail.toolConfirmation.missingItems" class="warn-text">
                    不达标/遗漏：{{ detail.toolConfirmation.missingItems }}
                  </p>
                  <p class="time">{{ detail.toolConfirmation.confirmedAt }}</p>
                </template>
                <el-empty v-else description="待理发师上门扫码核验" :image-size="40" />
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
              <el-table-column label="类型" width="150">
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

          <!-- 备用服务包 -->
          <el-card v-if="canConfirmTools && detail.spareKits?.length" shadow="never" style="margin-top: 16px">
            <template #header>社区备用服务包（核验不达标时启用）</template>
            <div v-for="s in detail.spareKits" :key="s.kit.id" class="spare-item">
              <div>
                <b>{{ s.kit.name }}</b>
                <el-tag size="small" :type="s.checks.length ? 'danger' : 'success'" style="margin-left: 6px">
                  {{ s.checks.length ? '不可用' : '可启用' }}
                </el-tag>
              </div>
              <div class="muted">封签 {{ s.kit.sealCode || '—' }}；消毒 {{ s.kit.disinfectedAt }}</div>
              <div v-if="s.checks.length" class="warn-text">{{ s.checks.join('；') }}</div>
              <el-button size="small" type="warning" :disabled="s.checks.length > 0"
                         @click="doActivateSpare(s.kit.id)">启用此备用包</el-button>
            </div>
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

    <!-- 上门前扫码核验对话框 -->
    <el-dialog v-model="toolDialog" title="上门前扫码核验服务包" width="640px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px"
        title="先扫描服务包封签二维码；消毒超期、封签破损、毛巾围布受潮、剪刀剃刀污渍或工具遗漏时，不允许开始服务，请联系社区改约或启用备用服务包。" />
      <el-form label-width="120px">
        <el-form-item label="扫描/输入封签号">
          <el-input v-model="toolForm.sealCode" placeholder="如 SEAL-LF-20260917-01" style="width: 320px">
            <template #append><el-button @click="doScan">扫码核验</el-button></template>
          </el-input>
        </el-form-item>
        <div v-if="scannedKit" class="scan-box">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="服务包">{{ scannedKit.kit.name }}</el-descriptions-item>
            <el-descriptions-item label="消毒日期">{{ scannedKit.kit.disinfectedAt }}</el-descriptions-item>
            <el-descriptions-item label="消毒方式">{{ DISINFECTION_METHOD_LABELS[scannedKit.kit.disinfectionMethod] || '—' }}</el-descriptions-item>
            <el-descriptions-item label="消毒柜编号">{{ scannedKit.kit.cabinetNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="责任人">{{ scannedKit.kit.responsiblePerson || '—' }}</el-descriptions-item>
            <el-descriptions-item label="核验结论">
              <el-tag v-if="scannedKit.checks.length" type="danger">不通过：{{ scannedKit.checks.join('；') }}</el-tag>
              <el-tag v-else type="success">系统记录达标</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-divider content-position="left">逐项现场检查</el-divider>
        <el-form-item label="封签完好"><el-switch v-model="toolForm.sealIntact" active-text="完好" inactive-text="破损" /></el-form-item>
        <el-form-item label="消毒在有效期"><el-switch v-model="toolForm.disinfectionValid" active-text="有效" inactive-text="过期" /></el-form-item>
        <el-form-item label="消毒方式可查"><el-switch v-model="toolForm.methodOk" /></el-form-item>
        <el-form-item label="消毒柜编号可查"><el-switch v-model="toolForm.cabinetOk" /></el-form-item>
        <el-form-item label="责任人可查"><el-switch v-model="toolForm.responsibleOk" /></el-form-item>
        <el-form-item label="理发工具齐备"><el-switch v-model="toolForm.toolsOk" /></el-form-item>
        <el-form-item label="剪刀剃刀无污渍"><el-switch v-model="toolForm.toolsClean" active-text="清洁" inactive-text="有污渍" /></el-form-item>
        <el-form-item label="围布齐备"><el-switch v-model="toolForm.capeOk" /></el-form-item>
        <el-form-item label="毛巾围布干燥"><el-switch v-model="toolForm.clothDry" active-text="干燥" inactive-text="受潮" /></el-form-item>
        <el-form-item label="消毒用品齐备"><el-switch v-model="toolForm.disinfectantOk" /></el-form-item>
        <el-form-item label="服务包齐备"><el-switch v-model="toolForm.packOk" /></el-form-item>
        <el-form-item label="不达标说明"><el-input v-model="toolForm.missingItems" placeholder="如有遗漏/不达标请说明" /></el-form-item>

        <el-divider content-position="left">感染风险（头癣/皮肤病/开放性伤口/要求一次性用品）</el-divider>
        <el-form-item label="标记感染风险"><el-switch v-model="toolForm.infectionRisk" /></el-form-item>
        <template v-if="toolForm.infectionRisk">
          <el-form-item label="风险原因">
            <el-select v-model="toolForm.infectionRiskReason" style="width: 100%">
              <el-option label="老人有头癣" value="老人有头癣" />
              <el-option label="老人有皮肤病" value="老人有皮肤病" />
              <el-option label="老人有开放性伤口" value="老人有开放性伤口" />
              <el-option label="老人明确要求使用一次性用品" value="老人明确要求使用一次性用品" />
            </el-select>
          </el-form-item>
          <el-form-item label="工具单独分装"><el-switch v-model="toolForm.toolsSeparated" /></el-form-item>
          <el-form-item label="已用一次性用品"><el-switch v-model="toolForm.disposableUsed" /></el-form-item>
          <el-form-item label="用后处理方式">
            <el-input v-model="toolForm.postUseHandling" type="textarea" :rows="2"
                      placeholder="如：一次性用品按医废回收；重复工具回站含氯浸泡加强消毒并单独封装备查" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="toolDialog = false">取消</el-button>
        <el-button type="danger" plain @click="toolIssueDialog = true">核验不达标：联系社区</el-button>
        <el-button type="primary" @click="doConfirmTools">提交核验</el-button>
      </template>
    </el-dialog>

    <!-- 核验不达标处置：改约 / 取消 / 启用备用包 -->
    <el-dialog v-model="toolIssueDialog" title="核验不达标处置（不记老人违约、不扣补贴）" width="460px">
      <el-radio-group v-model="toolIssueAction" style="margin-bottom: 12px">
        <el-radio value="reschedule">联系社区改约</el-radio>
        <el-radio value="cancel">取消（标记理发师空跑）</el-radio>
      </el-radio-group>
      <el-form label-width="90px">
        <template v-if="toolIssueAction === 'reschedule'">
          <el-form-item label="新日期"><el-date-picker v-model="toolIssueForm.newDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          <el-form-item label="新时段">
            <el-select v-model="toolIssueForm.newTimeSlot" style="width: 100%">
              <el-option v-for="s in TIME_SLOTS" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="理发师空跑"><el-switch v-model="toolIssueForm.emptyRun" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="toolIssueForm.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="toolIssueDialog = false">返回</el-button>
        <el-button type="primary" @click="doToolIssue">确认</el-button>
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

    <!-- 感染反馈对话框 -->
    <el-dialog v-model="infectionDialog" title="反馈皮肤瘙痒/红疹/感染" width="460px">
      <el-alert type="error" :closable="false" show-icon style="margin-bottom: 12px"
        title="提交后社区将按理发师、服务包、工具编号和同日服务记录反查，通知同工具服务老人的家属和志愿者并建议就医。" />
      <el-input v-model="infectionText" type="textarea" :rows="4"
                placeholder="请描述症状（瘙痒/红疹/感染）、出现时间、是否已就医" />
      <template #footer>
        <el-button @click="infectionDialog = false">取消</el-button>
        <el-button type="danger" @click="doReportInfection">提交反馈并发起追溯</el-button>
      </template>
    </el-dialog>

    <!-- 空跑补偿认定 -->
    <el-dialog v-model="compDialog" title="理发师空跑补偿认定" width="460px">
      <el-form label-width="110px">
        <el-form-item label="认定结果">
          <el-radio-group v-model="compForm.result">
            <el-radio value="COMMUNITY_APPROVED">社区规则公益资金补偿</el-radio>
            <el-radio value="BARBER_BORNE">工具遗漏/消毒失责，理发师承担</el-radio>
            <el-radio value="WAIVED">无需补偿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补偿金额" v-if="compForm.result === 'COMMUNITY_APPROVED'">
          <el-input-number v-model="compForm.amount" :min="0" :max="100" /> 元
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="compForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="compDialog = false">取消</el-button>
        <el-button type="primary" @click="doAdjudicate">确认认定</el-button>
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
  scanSeal, activateSpareKit, rescheduleToolIssue, cancelToolIssue, adjudicateCompensation,
  createInfectionCase
} from '../api'
import { useUserStore } from '../store/user'
import PhotoUpload from '../components/PhotoUpload.vue'
import PhotoView from '../components/PhotoView.vue'
import {
  ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, ORDER_TYPE_LABELS, RISK_LABELS, RISK_TYPES,
  PAYMENT_LABELS, PAYMENT_TYPES, EXCEPTION_TYPE_LABELS, EXCEPTION_STATUS_LABELS,
  EXCEPTION_STATUS_TYPES, SUBSIDY_STATUS_LABELS, SUBSIDY_STATUS_TYPES,
  FOLLOWUP_TYPE_LABELS, FOLLOWUP_STATUS_LABELS, ROLE_LABELS, EVENT_TYPE_LABELS, TIME_SLOTS,
  DISINFECTION_METHOD_LABELS, COMPENSATION_STATUS_LABELS, COMPENSATION_STATUS_TYPES
} from '../utils/labels'

const route = useRoute()
const store = useUserStore()
const id = route.params.id

const loading = ref(false)
const detail = ref({})
const exceptions = ref([])
const activePanels = ref(['tool', 'visit', 'record'])

const toolDialog = ref(false)
const toolIssueDialog = ref(false)
const toolIssueAction = ref('reschedule')
const scannedKit = ref(null)
const checkInDialog = ref(false)
const completeDialog = ref(false)
const rateDialog = ref(false)
const exceptionDialog = ref(false)
const infectionDialog = ref(false)
const compDialog = ref(false)
const rescheduleDialog = ref(false)
const cancelDialog = ref(false)
const cancelReason = ref('')
const infectionText = ref('')

const toolForm = reactive({
  sealCode: '', sealIntact: true, disinfectionValid: true, methodOk: true, cabinetOk: true, responsibleOk: true,
  toolsOk: true, toolsClean: true, capeOk: true, clothDry: true, disinfectantOk: true, packOk: true,
  missingItems: '', infectionRisk: false, infectionRiskReason: '老人有皮肤病',
  toolsSeparated: false, postUseHandling: '', disposableUsed: false
})
const toolIssueForm = reactive({ newDate: '', newTimeSlot: '09:00-11:00', reason: '', emptyRun: true })
const checkInForm = reactive({ entrySafe: true, elderState: '', mentalState: '良好', familyAuthorized: false, photos: [], notes: '' })
const completeForm = reactive({ photos: [], paymentNote: '' })
const rateForm = reactive({ rating: 5, comment: '' })
const exceptionForm = reactive({ type: 'ELDER_UNWELL', description: '' })
const rescheduleForm = reactive({ newDate: '', newTimeSlot: '09:00-11:00', reason: '' })
const compForm = reactive({ result: 'COMMUNITY_APPROVED', amount: 20, note: '' })

const order = computed(() => detail.value.order)
const isFinished = computed(() => ['COMPLETED', 'CANCELLED'].includes(order.value?.status))
const isBarberOfOrder = computed(() =>
  store.role === 'BARBER' && order.value?.barberId === store.user?.id)
const isVolunteerOfOrder = computed(() =>
  store.role === 'VOLUNTEER' && order.value?.volunteerId === store.user?.id)
const staffLike = computed(() => ['ADMIN', 'STAFF'].includes(store.role))

const canConfirmTools = computed(() =>
  (isBarberOfOrder.value || staffLike.value) && ['ASSIGNED', 'TOOL_CONFIRMED'].includes(order.value?.status))
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
const canReportInfection = computed(() => order.value?.status !== 'CANCELLED')
const canAdjudicate = computed(() =>
  staffLike.value || store.role === 'FINANCE')

const tagType = (ok) => (ok ? 'success' : 'danger')

const load = async () => {
  loading.value = true
  try {
    detail.value = await orderDetail(id)
    exceptions.value = await listOrderExceptions(id)
    if (detail.value.toolConfirmation?.sealCode) {
      toolForm.sealCode = detail.value.toolConfirmation.sealCode
    }
  } finally {
    loading.value = false
  }
}

const openToolDialog = () => {
  scannedKit.value = null
  toolDialog.value = true
}

const doScan = async () => {
  if (!toolForm.sealCode) {
    ElMessage.warning('请输入或扫描封签编号')
    return
  }
  try {
    scannedKit.value = await scanSeal(order.value.barberId, toolForm.sealCode)
    const k = scannedKit.value
    toolForm.sealIntact = k.kit.sealStatus === 'INTACT'
    toolForm.disinfectionValid = k.effectiveStatus === 'DISINFECTED'
    toolForm.methodOk = !!k.kit.disinfectionMethod
    toolForm.cabinetOk = !!k.kit.cabinetNo
    toolForm.responsibleOk = !!k.kit.responsiblePerson
    ElMessage.success(k.checks.length ? '系统记录存在不达标项' : '扫码核验：系统记录达标')
  } catch (e) {
    scannedKit.value = null
    // request 拦截器已提示
  }
}

const doConfirmTools = async () => {
  try {
    await confirmTools(id, { ...toolForm })
    ElMessage.success('扫码核验通过，可上门服务')
    toolDialog.value = false
    load()
  } catch (e) {
    // 核验未通过：后端返回拦截原因，保留对话框引导改约/备用包
    load()
  }
}

const doActivateSpare = async (kitId) => {
  await ElMessageBox.confirm('确认启用该备用服务包？使用后须补录消毒，未补录前该备用包不得再次使用。', '启用备用包', { type: 'warning' })
  await activateSpareKit(id, { spareKitId: kitId })
  ElMessage.success('已启用备用服务包')
  load()
}

const doToolIssue = async () => {
  if (toolIssueAction.value === 'reschedule') {
    if (!toolIssueForm.newDate) {
      ElMessage.warning('请选择改约日期')
      return
    }
    await rescheduleToolIssue(id, { ...toolIssueForm })
    ElMessage.success('已联系社区改约（不记老人违约、不扣补贴）')
  } else {
    await cancelToolIssue(id, { reason: toolIssueForm.reason || '工具核验不达标', emptyRun: toolIssueForm.emptyRun })
    ElMessage.success('已因工具问题取消，理发师空跑待社区认定补偿')
  }
  toolIssueDialog.value = false
  toolDialog.value = false
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

const doReportInfection = async () => {
  if (!infectionText.value) {
    ElMessage.warning('请描述症状')
    return
  }
  await createInfectionCase({ orderId: Number(id), symptom: '皮肤瘙痒/红疹/感染反馈', description: infectionText.value })
  ElMessage.success('已发起交叉感染追溯，社区将反查同工具服务老人并通知家属/志愿者')
  infectionDialog.value = false
  infectionText.value = ''
  load()
}

const doAdjudicate = async () => {
  await adjudicateCompensation(id, { ...compForm })
  ElMessage.success('补偿认定完成')
  compDialog.value = false
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
.scan-box {
  margin-bottom: 12px;
}
.spare-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
</style>
