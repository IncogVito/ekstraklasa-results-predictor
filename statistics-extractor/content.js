(function() {
  function createControl() {
    if (!document.body) return;
    if (document.getElementById('statistics-extractor-control')) return;

    const btn = document.createElement('button');
    btn.id = 'statistics-extractor-control';
    btn.textContent = 'Stat.';
    Object.assign(btn.style, {
      position: 'fixed',
      right: '16px',
      bottom: '16px',
      zIndex: '2147483647',
      padding: '8px 12px',
      borderRadius: '6px',
      background: '#0b5fff',
      color: '#fff',
      border: 'none',
      boxShadow: '0 2px 6px rgba(0,0,0,0.3)',
      cursor: 'pointer',
      fontSize: '13px'
    });

    btn.addEventListener('click', () => {
      const panelId = 'statistics-extractor-panel';
      let panel = document.getElementById(panelId);
      if (panel) {
        panel.remove();
        return;
      }
      panel = document.createElement('div');
      panel.id = panelId;
      Object.assign(panel.style, {
        position: 'fixed',
        right: '16px',
        bottom: '60px',
        width: '320px',
        height: '200px',
        zIndex: '2147483647',
        background: '#fff',
        color: '#000',
        border: '1px solid #ccc',
        borderRadius: '6px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.2)',
        padding: '12px',
        overflow: 'auto',
        fontSize: '13px'
      });
      panel.textContent = 'Panel kontrolny rozszerzenia';
      document.body.appendChild(panel);
    });

    document.body.appendChild(btn);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createControl);
  } else {
    createControl();
  }
})();

(async function ekstraklasaSeasonScraper() {
    /**********************
     * UI – status box
     **********************/
    function createStatusBox() {
        const statusBox = document.createElement("div");
        statusBox.id = "ekstraklasa-scraper-status";
        statusBox.style.position = "fixed";
        statusBox.style.bottom = "20px";
        statusBox.style.right = "20px";
        statusBox.style.zIndex = "99999";
        statusBox.style.padding = "12px 16px";
        statusBox.style.background = "rgba(0,0,0,0.8)";
        statusBox.style.color = "#fff";
        statusBox.style.fontSize = "12px";
        statusBox.style.fontFamily = "monospace";
        statusBox.style.borderRadius = "8px";
        statusBox.innerText = "⏳ Inicjalizacja…";
        document.body.appendChild(statusBox);
        return statusBox;
    }

    function updateStatus(message) {
        statusBox.innerText = message;
    }

    const statusBox = createStatusBox();

    /**********************
     * Helpers
     **********************/
    function sleep(milliseconds) {
        return new Promise(resolve => setTimeout(resolve, milliseconds));
    }

    function waitForElement(selector, timeout = 10000) {
        return new Promise((resolve, reject) => {
            const startTime = Date.now();
            const interval = setInterval(() => {
                const element = document.querySelector(selector);
                if (element) {
                    clearInterval(interval);
                    resolve(element);
                }
                if (Date.now() - startTime > timeout) {
                    clearInterval(interval);
                    reject(`Timeout waiting for ${selector}`);
                }
            }, 300);
        });
    }

    /**********************
     * Data extraction
     **********************/
    function getClubElementsFromLeagueTable() {
        return Array.from(
            document.querySelectorAll(
                ".stat-group.stat-box.league-table-mini .leagueTableTeamName.al"
            )
        );
    }

    function getMatchElementsFromHistory() {
        return Array.from(
            document.querySelectorAll(
                "#matchHistoryWidget .matchHistoryEvent:not(.incomplete)"
            )
        );
    }

    function extractMatchTime() {
        const timeElement = document.querySelector(
            ".match-info .timezone-convert-match-h2h-neo"
        );
        return timeElement ? timeElement.innerText.trim() : null;
    }

    function extractTeamsFromTableHeader() {
        const headerRow = document.querySelector(
            ".stat-group.stat-box thead tr.row.header"
        );
        if (!headerRow) return null;

        const teamLinks = headerRow.querySelectorAll("th.item.stat a.black");
        return {
            homeTeam: teamLinks[0]?.innerText.trim() || null,
            awayTeam: teamLinks[1]?.innerText.trim() || null
        };
    }

    function extractFinalScore() {
        const scoreElement = document.querySelector(
            ".stat-group.stat-box .row div p"
        );
        return scoreElement ? scoreElement.innerText.trim() : null;
    }

    function extractStatisticsFromTableBody() {
        const statistics = {};
        const statRows = document.querySelectorAll(
            ".stat-group.stat-box tbody tr.row"
        );

        statRows.forEach(row => {
            const key = row.querySelector("td.item.key")?.innerText.trim();
            const values = row.querySelectorAll("td.item.stat");

            if (!key || values.length < 2) return;

            statistics[key] = {
                home: values[0].innerText.trim(),
                away: values[1].innerText.trim()
            };
        });

        return statistics;
    }

    function extractMatchData() {
        const teams = extractTeamsFromTableHeader();

        return {
            matchTime: extractMatchTime(),
            homeTeam: teams?.homeTeam,
            awayTeam: teams?.awayTeam,
            finalScore: extractFinalScore(),
            statistics: extractStatisticsFromTableBody(),
            extractedAt: new Date().toISOString()
        };
    }

    /**********************
     * Main logic
     **********************/
    const extractedMatches = [];
    const visitedMatchKeys = new Set();

    const clubElements = getClubElementsFromLeagueTable();
    updateStatus(`🏟️ Kluby znalezione: ${clubElements.length}`);

    for (let clubIndex = 0; clubIndex < clubElements.length; clubIndex++) {
        const clubElement = clubElements[clubIndex];
        const clubName = clubElement.innerText.trim();

        updateStatus(`➡️ Klub (${clubIndex + 1}/${clubElements.length}): ${clubName}`);
        clubElement.click();

        await sleep(1500);
        await waitForElement("#matchHistoryWidget");

        const matchElements = getMatchElementsFromHistory();
        updateStatus(`⚽ Mecze: ${matchElements.length}`);

        for (let matchIndex = 0; matchIndex < matchElements.length; matchIndex++) {
            const matchElement = matchElements[matchIndex];

            updateStatus(
                `📊 ${clubName} – mecz ${matchIndex + 1}/${matchElements.length}`
            );

            matchElement.click();
            await sleep(1500);
            await waitForElement(".stat-group.stat-box");

            const matchData = extractMatchData();

            const matchKey = `${matchData.matchTime}-${matchData.homeTeam}-${matchData.awayTeam}`;
            if (!visitedMatchKeys.has(matchKey)) {
                visitedMatchKeys.add(matchKey);
                extractedMatches.push(matchData);
            }
        }
    }

    /**********************
     * Export
     **********************/
    window.extractedEkstraklasaMatches = extractedMatches;
    updateStatus(`✅ Gotowe! Mecze: ${extractedMatches.length}`);

    console.log("📦 Ekstraklasa JSON:", extractedMatches);
})();