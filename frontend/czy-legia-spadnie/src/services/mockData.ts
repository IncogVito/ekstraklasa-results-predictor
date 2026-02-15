import type { Team, Match, Standings } from '../types';

const TEAMS: Team[] = [
    { id: 'LEG', name: 'Legia Warszawa', played: 20, points: 32 },
    { id: 'JAG', name: 'Jagiellonia Białystok', played: 20, points: 41 },
    { id: 'SLA', name: 'Śląsk Wrocław', played: 20, points: 38 },
    { id: 'LPO', name: 'Lech Poznań', played: 20, points: 36 },
    { id: 'RAK', name: 'Raków Częstochowa', played: 20, points: 35 },
    { id: 'POG', name: 'Pogoń Szczecin', played: 20, points: 33 },
    { id: 'GZA', name: 'Górnik Zabrze', played: 20, points: 29 },
    { id: 'STA', name: 'Stal Mielec', played: 20, points: 28 },
    { id: 'ZAG', name: 'Zagłębie Lubin', played: 20, points: 25 },
    { id: 'WID', name: 'Widzew Łódź', played: 20, points: 25 },
    { id: 'PIA', name: 'Piast Gliwice', played: 20, points: 24 },
    { id: 'RAD', name: 'Radomiak Radom', played: 20, points: 24 },
    { id: 'CRA', name: 'Cracovia', played: 20, points: 21 },
    { id: 'WAR', name: 'Warta Poznań', played: 20, points: 20 },
    { id: 'KOR', name: 'Korona Kielce', played: 20, points: 20 },
    { id: 'PUS', name: 'Puszcza Niepołomice', played: 20, points: 19 },
    { id: 'RUCH', name: 'Ruch Chorzów', played: 20, points: 15 },
    { id: 'LKS', name: 'ŁKS Łódź', played: 20, points: 10 }
];

const UPCOMING_MATCHES: Match[] = [
    // Kolejka 21
    { id: '1', homeTeam: 'Stal Mielec', awayTeam: 'ŁKS Łódź', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-09T18:00:00' },
    { id: '2', homeTeam: 'Ruch Chorzów', awayTeam: 'Górnik Zabrze', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-09T20:30:00' },
    { id: '3', homeTeam: 'Piast Gliwice', awayTeam: 'Raków Częstochowa', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-10T15:00:00' },
    { id: '4', homeTeam: 'Lech Poznań', awayTeam: 'Zagłębie Lubin', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-10T17:30:00' },
    { id: '5', homeTeam: 'Cracovia', awayTeam: 'Radomiak Radom', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-10T20:00:00' },
    { id: '6', homeTeam: 'Pogoń Szczecin', awayTeam: 'Śląsk Wrocław', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-11T12:30:00' },
    { id: '7', homeTeam: 'Warta Poznań', awayTeam: 'Korona Kielce', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-11T15:00:00' },
    { id: '8', homeTeam: 'Widzew Łódź', awayTeam: 'Jagiellonia Białystok', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-11T17:30:00' },
    { id: '9', homeTeam: 'Puszcza Niepołomice', awayTeam: 'Legia Warszawa', homeScore: null, awayScore: null, matchday: 21, date: '2024-02-11T16:00:00' }, // Adjusted time logic

    // Kolejka 22
    { id: '10', homeTeam: 'Legia Warszawa', awayTeam: 'Widzew Łódź', homeScore: null, awayScore: null, matchday: 22, date: '2024-02-16T20:30:00' },
    { id: '11', homeTeam: 'Jagiellonia Białystok', awayTeam: 'Ruch Chorzów', homeScore: null, awayScore: null, matchday: 22, date: '2024-02-17T15:00:00' },
    { id: '12', homeTeam: 'Śląsk Wrocław', awayTeam: 'Lech Poznań', homeScore: null, awayScore: null, matchday: 22, date: '2024-02-17T17:30:00' },
];

export const getInitialStandings = (): Standings => {
    return {
        updatedAt: new Date().toISOString(),
        table: [...TEAMS].sort((a, b) => b.points - a.points)
    };
};

export const getUpcomingMatches = (): Match[] => {
    return [...UPCOMING_MATCHES];
};

export const getHistoricalProbability = () => {
    return [
        { week: 15, probability: 0.5 },
        { week: 16, probability: 0.8 },
        { week: 17, probability: 1.2 },
        { week: 18, probability: 0.9 },
        { week: 19, probability: 2.1 },
        { week: 20, probability: 1.8 },
        { week: 21, probability: 45.5 }, // Current
    ];
};

// Helper to clone teams to avoid mutating original state
export const getCleanStandings = (): Team[] => {
    return JSON.parse(JSON.stringify(TEAMS));
};

export const updateStandings = (matches: Match[]): Team[] => {
    // Start with fresh standings
    const standings = getCleanStandings();
    
    // Create a map for fast lookup
    const teamsMap = new Map<string, Team>();
    standings.forEach(t => teamsMap.set(t.name, t));

    matches.forEach(match => {
        // Only process matches with full scores
        if (match.homeScore !== null && match.homeScore !== undefined && 
            match.awayScore !== null && match.awayScore !== undefined) {
            
            const home = teamsMap.get(match.homeTeam);
            const away = teamsMap.get(match.awayTeam);

            if (home && away) {
                home.matches += 1;
                away.matches += 1;

                if (match.homeScore > match.awayScore) {
                    home.points += 3;
                    home.won += 1;
                    away.lost += 1;
                } else if (match.homeScore < match.awayScore) {
                    away.points += 3;
                    away.won += 1;
                    home.lost += 1;
                } else {
                    home.points += 1;
                    away.points += 1;
                    home.drawn += 1;
                    away.drawn += 1;
                }

                // Ideally we'd update goals too, but Team interface might need it.
                // For now, points are the main thing users watch for "relegation".
            }
        }
    });

    // Re-sort standings
    return standings.sort((a, b) => b.points - a.points);
};

export const calculateRelegationProbability = (matches: Match[]): number => {
    // Mock logic: 
    // Base probability of Legia relegating is low ~1-5%
    // If they lose the next match in our editor, bump it up.
    // If they win, bump it down.

    const legiaMatch = matches.find(m => m.homeTeam.includes('Legia') || m.awayTeam.includes('Legia'));

    let probability = 42.5; // Base mock percentage

    if (legiaMatch) {
        const isHome = legiaMatch.homeTeam.includes('Legia');
        const legiaScore = isHome ? legiaMatch.homeScore : legiaMatch.awayScore;
        const oppScore = isHome ? legiaMatch.awayScore : legiaMatch.homeScore;

        if (legiaScore !== null && oppScore !== null && legiaScore !== undefined && oppScore !== undefined) {
            if (legiaScore < oppScore) {
                probability = 15.0; // Spadamy! (not really, but mock updated)
            } else if (legiaScore > oppScore) {
                probability = 0.1; // Winning makes us safe
            } else {
                probability = 5.0; // Draw is meh
            }
        }
    }

    return probability;
};
