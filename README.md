# Sentiment Analysis Web Application (Java back-end + browser dashboard)

Subject: Social Media Analytics. Two real analyzers you can switch between at runtime:
1. **Lexicon (rule based)** — word scores, negation, boosters, CAPS, "!", "but" (VADER-style).
2. **Naive Bayes (machine learning)** — trained inside the app on a labelled CSV, so it really learns.

## Run (needs JDK 17 or newer)

| System | Command |
|---|---|
| Windows | double-click `run.bat` |
| Linux / macOS | `sh run.sh` |

It starts a small Java web server and opens **http://localhost:8080** in your browser (or open it yourself
if it doesn't). To use a different port: `java Main.java 9090` (from `src/sentiment/`) or `java -jar SentimentApp.jar 9090`.

Everything you do (history, your lexicon edits, an uploaded training set) is saved in the `data/` folder as
plain CSV files, so it survives a restart. Delete a file there to reset that part.

## Self test (no server needed)

```
javac -d out $(find src -name "*.java")
cp -r data web out/
java -cp out sentiment.test.EngineSelfTest
```
Checks the lexicon rules (negation, boosters, CAPS, "!"), a JSON/CSV round trip, and the Naive Bayes
classifier's accuracy on a held-out test split of the bundled dataset.

## Pages

1. **Analyze** one text, with a highlighted breakdown, and a live "social feed" simulator.
2. **Batch / CSV** many lines at once, or upload a CSV (any `text`/`review`/`comment`/`post`/`tweet` column).
3. **Dashboard** totals, a score trend chart, sentiment share, and the top positive/negative words seen so far
   (all computed live from your history — nothing is fixed).
4. **History** search, filter by label, delete entries.
5. **Lexicon** add, edit or remove words at runtime; changes apply to the next analysis immediately.
6. **Model Lab** see the current training dataset, upload your own `text,label` CSV to retrain Naive Bayes,
   and run a live evaluation (accuracy, precision, recall, F1, confusion matrix) comparing both analyzers on
   a held-out test split.

## Why it is not static
* The lexicon is a CSV you can edit from the Lexicon page while the app is running.
* Naive Bayes is trained from data, not hand-coded: upload any labelled CSV and it relearns from it.
* The dashboard, history and evaluation numbers are computed from what you actually analyzed, not fixed demo numbers.
* The live feed picks a random post from `data/feed.csv` each time, so no run looks the same.

## Project structure

```
src/sentiment/analyzer   Analyzer (interface), LexiconAnalyzer, NaiveBayesAnalyzer, Tokenizer, Labels, Dataset
src/sentiment/service    AnalysisService (all business logic), Evaluation
src/sentiment/storage    HistoryStore, LexiconStore (both persisted as CSV in data/)
src/sentiment/web        Server (plain java.net HTTP server), ApiHandlers (REST), StaticHandler
src/sentiment/util       Json, Csv, Res  (small hand-written helpers, no external libraries)
web/                     index.html, app.js, style.css  (charts are drawn with plain <canvas>, no library)
data/                    lexicon.csv, training.csv (463 labelled rows), feed.csv (308 posts)
```
REST API: `/api/analyze`, `/api/batch`, `/api/upload`, `/api/feed/next`, `/api/stats`, `/api/history*`,
`/api/lexicon*`, `/api/dataset`, `/api/train`, `/api/evaluate`.
