# Dashboard API - Comprehensive Analytics Aggregation

## Overview
The Dashboard API consolidates all analytics from 5 peer modules (Subscriber, Plan, Usage, Billing, Fault) into a unified executive dashboard with KPI metrics and visualization-ready chart data.

---

## Dashboard Endpoints

### 1. **GET /api/dashboard** - Complete Dashboard
**Purpose**: Get the full executive dashboard with all metrics, charts, regional and segment breakdown

**URL**: `http://localhost:8089/teleConnect/api/dashboard`

**Query Parameters**:
- `cycleId` (optional, default=1): Billing cycle ID
- `startDate` (optional, format: YYYY-MM-DD): Period start date
- `endDate` (optional, format: YYYY-MM-DD): Period end date

**Example Request**:
```
GET /teleConnect/api/dashboard?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
```

**Response Structure**:
```json
{
  "status": "success",
  "message": "Dashboard data retrieved",
  "data": {
    "kpis": {...},
    "charts": {...},
    "regions": {...},
    "segments": {...}
  }
}
```

---

## KPI Metrics

### 2. **GET /api/dashboard/kpis** - KPI Only (Fast Endpoint)
**Purpose**: Quick fetch of key performance indicators only

**URL**: `http://localhost:8089/teleConnect/api/dashboard/kpis`

**Response**:
```json
{
  "activeSubscribers": 2000,
  "churnRate": 2.5,
  "arpu": 450.50,
  "collectionEfficiency": 94.5,
  "slaCompliance": 96.8,
  "dataConsumption": 15000,
  "faultCount": 42,
  "disputeRate": 2.5
}
```

**KPI Definitions**:
| KPI | Definition | Source |
|-----|-----------|--------|
| activeSubscribers | Total active subscriber accounts | Subscriber Service |
| churnRate | % of subscribers lost in period | Subscriber Service |
| arpu | Average Revenue Per User | Billing Service |
| collectionEfficiency | % of invoices collected | Billing Service |
| slaCompliance | % of faults resolved within SLA | Fault Service |
| dataConsumption | Total data consumed (MB) | Usage Service |
| faultCount | Total escalated fault tickets | Fault Service |
| disputeRate | % of disputed invoices | Billing Service |

---

## Chart Data for Visualizations

### 3. **GET /api/dashboard/charts** - Visualization Data
**Purpose**: Get chart-ready data for frontend graphs and dashboards

**URL**: `http://localhost:8089/teleConnect/api/dashboard/charts`

**Response Contains**:
```json
{
  "consumptionTrend": {
    "title": "Data Consumption Trend",
    "labels": ["Week 1", "Week 2", "Week 3", "Week 4"],
    "data": [3500, 4200, 3800, 4500]
  },
  "arpuByAccountType": {
    "title": "ARPU by Account Type",
    "labels": ["Prepaid", "Postpaid", "Enterprise"],
    "values": [350.50, 450.75, 650.25]
  },
  "churnBySegment": {
    "title": "Churn Distribution by Segment",
    "labels": ["Prepaid", "Postpaid", "Enterprise"],
    "values": [45.0, 35.0, 20.0]
  },
  "collectionOverdueAgeing": {
    "title": "Overdue Invoices by Age",
    "labels": ["0-30 days", "31-60 days", "60+ days"],
    "values": [120, 80, 35]
  },
  "subscriberGrowthTrend": {
    "title": "Subscriber Growth Trend",
    "labels": ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
    "data": [1000, 1200, 1450, 1650, 1850, 2000]
  },
  "faultFrequency": {
    "title": "Fault Frequency by Priority",
    "labels": ["Critical", "High", "Medium", "Low"],
    "values": [15.0, 35.0, 40.0, 10.0]
  }
}
```

**Chart Types Supported**:
- **Line Charts**: Time-series data (Consumption Trend, Subscriber Growth)
- **Bar Charts**: Categorical comparisons (ARPU by Account Type, Fault Frequency, Overdue Ageing)
- **Pie Charts**: Distribution analysis (Churn by Segment)

---

## Regional Analysis

### 4. **GET /api/dashboard/regions** - Regional Breakdown
**Purpose**: Get metrics aggregated by region

**URL**: `http://localhost:8089/teleConnect/api/dashboard/regions`

**Response**:
```json
{
  "subscribersByRegion": {
    "Region 1": 1200,
    "Region 2": 800
  },
  "churnByRegion": {
    "Region 1": 2.5,
    "Region 2": 3.1
  },
  "arpuByRegion": {
    "Region 1": 450.50,
    "Region 2": 425.75
  }
}
```

---

## Segment Analysis

### 5. **GET /api/dashboard/segments** - Segment Breakdown (by Account Type)
**Purpose**: Get metrics aggregated by subscriber segment/account type

**URL**: `http://localhost:8089/teleConnect/api/dashboard/segments`

**Response**:
```json
{
  "subscribersByAccountType": {
    "PREPAID": 900,
    "POSTPAID": 800,
    "ENTERPRISE": 300
  },
  "arpuByAccountType": {
    "PREPAID": 350.50,
    "POSTPAID": 450.75,
    "ENTERPRISE": 650.25
  },
  "churnByAccountType": {
    "PREPAID": 2.8,
    "POSTPAID": 2.5,
    "ENTERPRISE": 1.2
  }
}
```

---

## Postman Collection Format

### Import into Postman

**Base URL**: `http://localhost:8089/teleConnect`

#### Request 1: Complete Dashboard
```
GET {{base_url}}/api/dashboard?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
Headers: Content-Type: application/json
```

#### Request 2: KPI Metrics Only
```
GET {{base_url}}/api/dashboard/kpis?cycleId=1
Headers: Content-Type: application/json
```

#### Request 3: Chart Data
```
GET {{base_url}}/api/dashboard/charts?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
Headers: Content-Type: application/json
```

#### Request 4: Regional Analysis
```
GET {{base_url}}/api/dashboard/regions?cycleId=1
Headers: Content-Type: application/json
```

#### Request 5: Segment Analysis
```
GET {{base_url}}/api/dashboard/segments?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
Headers: Content-Type: application/json
```

---

## Data Flow Architecture

```
┌─────────────────────────────────────────────────────────┐
│         Dashboard Controller (DashboardController)       │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│         Dashboard Service (DashboardServiceImpl)          │
└──┬────────────┬────────────┬────────────┬────────────────┘
   │            │            │            │
   ▼            ▼            ▼            ▼
┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐
│ARPU  │   │Churn │   │Collection│SLA   │
│Service│  │Service│  │Efficiency│Service│
│      │   │      │   │Service   │      │
└──────┘   └──────┘   └──────┘   └──────┘
   │            │            │            │
   └────────────┼────────────┼────────────┘
                │
┌───────────────▼────────────────────────┐
│    Peer Module Clients                  │
├────────────────────────────────────────┤
│ • SubscriberStatsClient (Port 8081)    │
│ • BillingStatsClient (Port 8082)       │
│ • UsageStatsClient (Port 8083)         │
│ • FaultStatsClient (Port 8084)         │
└────────────────────────────────────────┘
```

---

## Features Implemented

### ✅ Dashboard Response DTOs
- **DashboardResponse**: Root response object
- **KPIMetrics**: 8 key performance indicators
- **ChartData**: 6 chart types for visualization
- **LineChartData**: Time-series visualizations
- **BarChartData**: Categorical comparisons
- **PieChartData**: Distribution analysis
- **RegionalAnalysis**: Regional breakdown
- **SegmentAnalysis**: Account type breakdown

### ✅ Service Layer
- **DashboardService Interface**: Defined contract
- **DashboardServiceImpl**: Implements aggregation logic
  - Calls all 4 peer service clients
  - Aggregates data from 4 analytics services (ARPU, Churn, Collection, SLA)
  - Provides regional and segment breakdowns

### ✅ Controller Layer
- **DashboardController**: 5 endpoints
  - GET /api/dashboard - Full dashboard
  - GET /api/dashboard/kpis - KPI metrics only
  - GET /api/dashboard/charts - Chart data
  - GET /api/dashboard/regions - Regional breakdown  
  - GET /api/dashboard/segments - Segment breakdown

### ✅ Data Integration
- Subscriber Module: Active subscribers, churn rate, account types, regions
- Billing Module: ARPU, collection efficiency, overdue aging, invoice statuses
- Usage Module: Data consumption trends
- Fault Module: SLA compliance, fault frequency, escalated tickets
- Plan Module: Derived from billing and subscriber data

### ✅ Visualization Support
- 6 different chart types
- Line charts for trends
- Bar charts for comparisons
- Pie charts for distributions
- All charts are frontend-ready (labels + values)

---

## Testing Checklist

### Unit Testing (Postman)
- [ ] GET /api/dashboard - Full response
- [ ] GET /api/dashboard/kpis - KPI metrics
- [ ] GET /api/dashboard/charts - Chart data
- [ ] GET /api/dashboard/regions - Regional data
- [ ] GET /api/dashboard/segments - Segment data
- [ ] Test with different cycleId values
- [ ] Test with custom date ranges
- [ ] Verify response time (should be <2s with mock data)

### Integration Testing  
- [ ] Verify mock clients provide test data
- [ ] Confirm all services inject correctly
- [ ] Check null handling for optional parameters
- [ ] Validate BigDecimal precision in ARPU values

---

## Performance Notes
- **KPI-only endpoint is fastest** (single aggregation pass)
- **Full dashboard includes all data** (comprehensive but slightly slower)
- **Regional/Segment endpoints are cached within main dashboard**
- **Mock clients respond instantly** (no network latency)

---

## Files Modified/Created

### Created Files
1. `src/main/java/com/teleconnect/analytics_service/dto/response/DashboardResponse.java`
2. `src/main/java/com/teleconnect/analytics_service/service/DashboardService.java`
3. `src/main/java/com/teleconnect/analytics_service/service/impl/DashboardServiceImpl.java`
4. `src/main/java/com/teleconnect/analytics_service/controller/DashboardController.java`

### Dependencies Used
- Spring Framework (Web, Data JPA)
- Existing client implementations (all 4 peer services)
- Existing service implementations (ARPU, Churn, Collection, SLA)
- Jackson for JSON serialization

---

## Future Enhancements
- [ ] Add export to PDF/Excel functionality
- [ ] Add scheduled report generation
- [ ] Add email notifications for alerts
- [ ] Add trend analysis (MoM, YoY comparisons)
- [ ] Add drill-down capabilities (segment → detailed reports)
- [ ] Add real-time data refresh (WebSocket support)
- [ ] Add caching layer for frequently accessed dashboards
