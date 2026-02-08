import axios from 'axios';
import * as fs from 'fs';
import * as path from 'path';

type AnyObj = { [k: string]: any };

const LEAGUE_URL = 'https://www.fotmob.com/api/data/leagues?id=196&ccode3=AUS';
const MATCH_DETAILS_URL = 'https://www.fotmob.com/api/data/matchDetails?matchId=';

function safeGet(obj: AnyObj, ...keys: string[]) {
    let cur: any = obj;
    for (const k of keys) {
        if (!cur) return undefined;
        cur = cur[k];
    }
    return cur;
}

function normalizeKey(item: AnyObj) {
    if (item.key) return item.key;
    if (item.title) return String(item.title).replace(/\s+/g, '_').toLowerCase();
    return undefined;
}

function collectStatsArray(items: any[], out: AnyObj) {
    if (!Array.isArray(items)) return;
    for (const item of items) {
        if (!item) continue;
        if (Array.isArray(item.stats)) {
            const statsArr = item.stats;
            // @ts-ignore
            const isPrimitiveArray = statsArr.every(s => s === null || ['string', 'number', 'boolean'].includes(typeof s));
            const key = normalizeKey(item);
            if (isPrimitiveArray && key && (statsArr.length >= 2)) {
                out[`${key}__home`] = statsArr[0] ?? '';
                out[`${key}__away`] = statsArr[1] ?? '';
            } else {
                // stats is likely an array of nested objects -> recurse
                collectStatsArray(statsArr, out);
            }
        }
    }
}

// Dodane: prosty parser linii CSV (obsługa cudzysłowów i podwójnych cudzysłowów)
function parseCsvLine(line: string): string[] {
    const fields: string[] = [];
    let i = 0;
    let cur = '';
    let inQuotes = false;
    while (i < line.length) {
        const ch = line[i];
        if (inQuotes) {
            if (ch === '"') {
                if (line[i + 1] === '"') { // escaped quote
                    cur += '"';
                    i += 2;
                    continue;
                } else {
                    inQuotes = false;
                    i++;
                    continue;
                }
            } else {
                cur += ch;
                i++;
                continue;
            }
        } else {
            if (ch === '"') {
                inQuotes = true;
                i++;
                continue;
            }
            if (ch === ',') {
                fields.push(cur);
                cur = '';
                i++;
                continue;
            }
            cur += ch;
            i++;
        }
    }
    fields.push(cur);
    return fields;
}

// Wczytuje istniejący CSV (jeśli istnieje) i zwraca: set id, tablicę obiektów (mapa nagłówek->wartość) oraz nagłówek
function readExistingCSV(fileName = 'fotmob_stats.csv') {
    const out = { ids: new Set<string>(), rows: [] as AnyObj[], header: [] as string[], exists: false, byId: {} as Record<string, AnyObj> };
    const p = path.resolve(process.cwd(), fileName);
    if (!fs.existsSync(p)) return out;
    out.exists = true;
    const content = fs.readFileSync(p, 'utf8');
    const lines = content.split(/\r?\n/);
    if (lines.length === 0) return out;
    // znajdź pierwszy nie-pusty wiersz jako header
    let headerLineIndex = 0;
    while (headerLineIndex < lines.length && lines[headerLineIndex].trim() === '') headerLineIndex++;
    if (headerLineIndex >= lines.length) return out;
    const header = parseCsvLine(lines[headerLineIndex]);
    out.header = header.map(h => h.trim());
    // znajdź indeks matchId w header
    const matchIdIndex = out.header.findIndex(h => h === 'matchId');

    for (let i = headerLineIndex + 1; i < lines.length; i++) {
        const line = lines[i];
        if (!line || line.trim() === '') continue;
        const fields = parseCsvLine(line);
        const obj: AnyObj = {};
        for (let j = 0; j < out.header.length; j++) {
            obj[out.header[j]] = fields[j] ?? '';
        }
        if (matchIdIndex !== -1) {
            const mid = fields[matchIdIndex] ?? '';
            if (mid) {
                const midStr = String(mid);
                out.ids.add(midStr);
                out.byId[midStr] = obj;
            }
        }
        out.rows.push(obj);
    }
    return out;
}

async function fetchLeagueMatches() {
    const res = await axios.get(LEAGUE_URL, {
        timeout: 20000,
        headers: {
            'User-Agent': 'PostmanRuntime/7.51.0'
        }
    });
    const overview = safeGet(res.data, 'overview');
    const matches = safeGet(overview, 'matches', 'allMatches') || safeGet(overview, 'matches') || [];
    return matches;
}

async function fetchMatchDetails(matchId: string) {
    const res = await axios.get(`${MATCH_DETAILS_URL}${matchId}`, {
        timeout: 20000,
        headers: {
            'User-Agent': 'PostmanRuntime/7.51.0'
        }
    });
    return res.data;
}

(async function main() {
    try {
        console.log('Fetching league matches...');
        const matches = await fetchLeagueMatches();
        console.log(`Found ${matches.length} matches (processing sequentially)...`);

        // parse CLI args
        const argv = process.argv.slice(2);
        const reset = argv.includes('--reset');
        if (reset) console.log('Reset mode enabled: will reprocess matches whose finished value is not true in CSV');

        // Wczytaj istniejący CSV (jeżeli jest)
        const existing = readExistingCSV();
        if (existing.exists) {
            console.log(`Found existing CSV with ${existing.rows.length} rows. Will consider ${existing.ids.size} existing matchId(s).`);
        }

        // Start from existing rows and allow replacing rows when reprocessing
        const statKeys = new Set<string>();
        const finalRows: AnyObj[] = existing.rows.slice();
        const idToIndex: Record<string, number> = {};
        for (let i = 0; i < finalRows.length; i++) {
            const r = finalRows[i];
            if (r && r.matchId) idToIndex[String(r.matchId)] = i;
        }

        var iterations = 0;
        for (const m of matches) {
            const matchId = String(m.id || m.matchId || '');
            if (!matchId) continue;

            if (existing.ids.has(matchId)) {
                if (reset) {
                    const existingRow = existing.byId[matchId];
                    const finishedVal = String(existingRow?.finished ?? '').toLowerCase();
                    if (finishedVal === 'true' || finishedVal === '1') {
                        console.log(`Skipping ${matchId} (already finished in CSV)`);
                        continue;
                    } else {
                        console.log(`Reprocessing ${matchId} (was not finished in CSV)`);
                    }
                } else {
                    console.log(`Skipping ${matchId} (already in CSV)`);
                    continue;
                }
            }
            iterations++;

            console.log(`Fetching details for match ${matchId}...`);
            let detail: AnyObj;
            try {
                detail = await fetchMatchDetails(matchId);
            } catch (err) {
                console.warn(`Failed to fetch details for ${matchId}: ${(err as any).message}`);
                continue;
            }

            const meta: AnyObj = {
                matchId,
                round: m.round ?? m.roundName ?? '',
                roundName: m.roundName ?? '',
                pageUrl: m.pageUrl ?? '',
                homeName: safeGet(m, 'home', 'name') ?? '',
                homeId: safeGet(m, 'home', 'id') ?? '',
                awayName: safeGet(m, 'away', 'name') ?? '',
                awayId: safeGet(m, 'away', 'id') ?? '',
                utcTime: safeGet(m, 'status', 'utcTime') ?? '',
                finished: safeGet(m, 'status', 'finished') ?? '',
                scoreStr: safeGet(m, 'status', 'scoreStr') ?? '',
            };

            const statsContainer = safeGet(detail!, 'content', 'stats', 'Periods', 'All', 'stats') || safeGet(detail, 'content', 'stats');
            const statsObj: AnyObj = {};
            if (statsContainer) {
                if (Array.isArray(statsContainer)) {
                    collectStatsArray(statsContainer, statsObj);
                } else if (Array.isArray((statsContainer as any).stats)) {
                    collectStatsArray((statsContainer as any).stats, statsObj);
                }
            }

            for (const k of Object.keys(statsObj)) statKeys.add(k);

            const newRow = { ...meta, ...statsObj };
            if (Object.prototype.hasOwnProperty.call(idToIndex, matchId)) {
                finalRows[idToIndex[matchId]] = newRow;
            } else {
                idToIndex[matchId] = finalRows.length;
                finalRows.push(newRow);
            }

            if (iterations > 5) {
                console.log('Limiting to 5 new matches per run for testing purposes.');
                break;
            }
        }

        // Build CSV - meta cols + discovered stat keys and existing extra cols
        const metaCols = ['matchId', 'round', 'roundName', 'pageUrl', 'homeName', 'homeId', 'awayName', 'awayId', 'utcTime', 'finished', 'scoreStr'];
        const newStatCols = Array.from(statKeys).sort();

        const existingHeader = existing.header || [];
        const otherExisting = existingHeader.filter(h => !metaCols.includes(h));
        const statUnion = Array.from(new Set([...newStatCols, ...otherExisting])).sort();
        const header = [...metaCols, ...statUnion];

        const csvLines: string[] = [];
        const escape = (v: any) => {
            if (v === undefined || v === null) return '';
            const s = String(v);
            if (s.includes('"') || s.includes(',') || s.includes('\n')) {
                return '"' + s.replace(/"/g, '""') + '"';
            }
            return s;
        };

        csvLines.push(header.join(','));
        const serialize = (obj: AnyObj) => header.map(h => escape(obj[h] ?? '')).join(',');

        for (const r of finalRows) {
            csvLines.push(serialize(r));
        }

        const outPath = path.resolve(process.cwd(), 'fotmob_stats.csv');
        fs.writeFileSync(outPath, csvLines.join('\n'), { encoding: 'utf8' });
        console.log(`Wrote ${finalRows.length} rows to ${outPath}`);
    } catch (err) {
        console.error('Fatal error:', err);
        process.exit(1);
    }
})();
