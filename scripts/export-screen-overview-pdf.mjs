import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";
import { chromium } from "@playwright/test";

const ROOT = process.cwd();
const SCREEN_DIR = path.resolve(ROOT, "screen");
const MANIFEST_PATH = path.join(SCREEN_DIR, "manifest.json");
const HTML_PATH = path.join(SCREEN_DIR, "overview-simple.html");
const PDF_PATH = path.join(SCREEN_DIR, "overview-simple.pdf");

const DESCRIPTIONS = {
  public: {
    "01_home": "首页，展示平台核心入口、精选内容与整体形象。",
    "02_platform": "平台介绍页，概览平台定位、能力与服务价值。",
    "03_onboarding": "企业入驻页，说明入驻流程与平台支持内容。",
    "04_products": "产品展示页，用于浏览平台公开产品列表。",
    "05_product_detail": "产品详情页，展示单个产品的详细信息与亮点。",
    "06_services": "服务市场页，用于浏览平台公开服务与筛选条件。",
    "07_service_detail": "服务详情页，展示单项服务的内容、报价与说明。",
    "08_providers": "服务商列表页，用于浏览已入驻服务商信息。",
    "09_provider_detail": "服务商详情页，展示服务商能力与服务范围。",
    "10_provider_join": "服务商入驻申请页，用于提交服务商资料。",
    "11_ai_tools": "AI 工具页，展示平台 AI 能力与示例场景。",
  },
  auth: {
    "01_login": "登录页，用于输入账号密码进入系统。",
    "02_register": "注册页，用于提交新账号注册信息。",
    "03_activate": "账号激活页，用于完成账户激活流程。",
    "04_forgot_password": "忘记密码页，用于找回或重置登录密码。",
  },
  enterprise: {
    "01_dashboard": "企业工作台页，概览企业侧核心业务入口与提醒信息。",
    "02_onboarding_apply": "企业入驻申请页，用于企业提交入驻资料。",
    "03_onboarding_submitted": "企业入驻提交结果页，用于展示申请提交状态。",
    "04_profile": "企业资料页，用于维护企业基础信息与展示资料。",
    "05_products": "企业产品管理页，用于查看和维护产品列表。",
    "06_product_new": "企业新建产品页，用于录入新的产品信息。",
    "07_product_preview": "企业产品预览页，用于查看单个产品展示效果。",
    "08_product_edit": "企业产品编辑页，用于修改已存在的产品信息。",
    "09_services": "企业服务市场页，用于浏览和采购可用服务。",
    "10_orders": "企业服务订单页，用于查看企业侧订单列表。",
    "11_order_detail": "企业订单详情页，用于查看订单状态与处理信息。",
    "12_payments": "企业支付记录页，用于查看支付与回单信息。",
    "13_deliveries": "企业交付管理页，用于跟踪服务交付进度。",
    "14_product_promotion": "企业产品推广页，用于查看推广服务与投放状态。",
    "15_import": "企业批量导入页，用于提交导入任务并查看结果。",
    "16_messages": "企业消息中心页，用于查看通知与协同消息。",
    "17_settings": "企业账号设置页，用于管理账户与安全配置。",
  },
  provider: {
    "01_dashboard": "服务商工作台页，概览服务商侧业务入口与状态信息。",
    "02_profile": "服务商资料页，用于维护服务商公司信息与简介。",
    "03_services": "服务商服务管理页，用于维护服务项目与报价内容。",
    "04_orders": "服务商订单管理页，用于查看待处理与执行中的订单。",
    "05_order_detail": "服务商订单详情页，用于查看单个订单的处理信息。",
    "06_fulfillment": "服务商交付管理页，用于跟踪交付节点与协作内容。",
  },
  admin: {
    "01_overview": "平台运营概览页，用于查看平台整体运行情况。",
    "02_users": "用户管理页，用于查看和管理平台用户账号。",
    "03_services": "平台服务管理页，用于管理服务目录与服务内容。",
    "04_service_orders": "平台服务订单管理页，用于查看全部服务订单。",
    "05_payments": "平台支付管理页，用于查看支付记录与审核状态。",
    "06_providers": "平台服务商管理页，用于查看服务商列表与状态。",
    "07_provider_reviews": "平台服务商审核页，用于处理服务商审核任务。",
    "08_fulfillment": "平台交付管理页，用于查看整体交付执行情况。",
    "09_marketplace_publish": "平台市场发布页，用于管理市场发布相关内容。",
    "10_company_reviews": "企业审核列表页，用于查看待审核企业申请。",
    "11_company_review_detail": "企业审核详情页，用于处理单个企业审核任务。",
    "12_companies": "企业管理页，用于查看和管理平台企业数据。",
    "13_product_reviews": "产品审核列表页，用于查看待审核产品。",
    "14_product_review_detail": "产品审核详情页，用于处理单个产品审核任务。",
    "15_products": "平台产品管理页，用于查看和管理产品数据。",
    "16_access_grant_requests": "权限申请页，用于处理系统访问授权申请。",
    "17_review_domains": "审核域分配页，用于管理审核域与审核员分配。",
    "18_categories": "基础类目配置页，用于维护平台类目结构。",
  },
};

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function getDescription(shot) {
  return DESCRIPTIONS[shot.section]?.[shot.name] ?? "页面截图，用于展示当前页面内容。";
}

function buildHtml(manifest) {
  const generatedAt = new Date().toLocaleString("zh-CN", { hour12: false });
  const pages = manifest.shots
    .map((shot) => {
      const relImage = path.relative(SCREEN_DIR, path.resolve(ROOT, shot.file)).replaceAll("\\", "/");
      return `
        <section class="page">
          <p class="desc">${escapeHtml(getDescription(shot))}</p>
          <div class="image-wrap">
            <img class="image" src="${escapeHtml(relImage)}" alt="${escapeHtml(shot.name)}" />
          </div>
        </section>
      `;
    })
    .join("");

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>MDM 页面截图总览</title>
    <style>
      @page {
        size: A4;
        margin: 12mm;
      }

      * {
        box-sizing: border-box;
      }

      body {
        margin: 0;
        font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
        color: #172033;
        background: #ffffff;
      }

      .page {
        min-height: calc(297mm - 24mm);
        page-break-after: always;
        display: flex;
        flex-direction: column;
      }

      .page:last-child {
        page-break-after: auto;
      }

      .desc {
        margin: 0 0 10mm;
        font-size: 16px;
        line-height: 1.7;
        font-weight: 600;
      }

      .image-wrap {
        flex: 1;
        display: flex;
        align-items: flex-start;
        justify-content: center;
        overflow: hidden;
      }

      .image {
        display: block;
        width: 100%;
        max-width: 186mm;
        max-height: 245mm;
        object-fit: contain;
        object-position: top center;
        border: 1px solid #d9dfeb;
        border-radius: 6px;
        background: #f8fafc;
      }

      .footer {
        position: fixed;
        right: 12mm;
        bottom: 6mm;
        font-size: 10px;
        color: #6b7280;
      }
    </style>
  </head>
  <body>
    ${pages}
    <div class="footer">生成时间：${escapeHtml(generatedAt)}</div>
  </body>
</html>`;
}

async function main() {
  const raw = await fs.readFile(MANIFEST_PATH, "utf8");
  const manifest = JSON.parse(raw);
  const html = buildHtml(manifest);

  await fs.writeFile(HTML_PATH, html, "utf8");

  let browser;
  try {
    browser = await chromium.launch({ channel: "msedge", headless: true });
  } catch {
    browser = await chromium.launch({ headless: true });
  }

  try {
    const page = await browser.newPage();
    await page.goto(pathToFileURL(HTML_PATH).href, { waitUntil: "load" });
    await page.emulateMedia({ media: "screen" });
    await page.pdf({
      path: PDF_PATH,
      format: "A4",
      printBackground: true,
      margin: {
        top: "12mm",
        right: "12mm",
        bottom: "12mm",
        left: "12mm",
      },
    });
  } finally {
    await browser.close();
  }

  console.log(`HTML: ${HTML_PATH}`);
  console.log(`PDF: ${PDF_PATH}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
