const { defineConfig } = require("@playwright/test");
const externalBaseURL = process.env.BASE_URL;
const baseURL = externalBaseURL || "http://127.0.0.1:8080";

module.exports = defineConfig({
    testDir: "./tests/e2e",
    fullyParallel: false,
    reporter: "list",
    use: {
        baseURL,
        headless: true,
        trace: "retain-on-failure"
    },
    webServer: externalBaseURL ? undefined : {
        command: process.platform === "win32" ? ".\\mvnw.cmd spring-boot:run" : "./mvnw spring-boot:run",
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120000
    }
});
