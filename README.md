# Fake News

A Java 21 / Spring Boot project that trains and compares Logistic Regression,
Random Forest, and Support Vector Machine (Weka SMO) classifiers on a labeled
Kaggle news CSV. It ranks candidates by **fake-class F1**, saves the winner,
and exposes it through a small web app.

## Dataset

Download the Kaggle Fake News dataset and place a labeled CSV at
`data/fake_news.csv`. The CSV must have a header containing `title`, `text`,
and `label`. Labels may be `fake`/`real` or `1`/`0` (fake is the positive
class). The dataset itself is not included in this repository.

## Build and train

Install Java 21 and Maven, then run from this project directory:

```sh
mvn test
mvn package
java -jar target/fake-news-detection-1.0.0.jar --train --spring.main.web-application-type=none
```

Training prints accuracy, fake-class precision/recall/F1, and a confusion
matrix for each candidate. The best model is refit on the full dataset and
written to `models/fake-news.model`.

Use a different CSV path with `--data=path/to/news.csv`.

## Run the web app

After training:

```sh
java -jar target/fake-news-detection-1.0.0.jar
```

Open [http://localhost:8080](http://localhost:8080). The JSON endpoint is
`POST /api/predict` with `{"text":"A headline or article"}`. The response
includes `predictedClass`, `confidence`, `probabilityFake`, and the most
prominent matching TF-IDF terms. Confidence is the probability assigned to
the predicted class; term prominence is based on the input's TF-IDF weights,
not a causal explanation of the classifier.

## Project layout

* `data` — CSV loading and article record
* `preprocessing` — tokenization, stopword removal, and lightweight
  rule-based lemmatization (no external language model required)
* `features` — fitted unigram/bigram TF-IDF vocabulary and transforms
* `training` — Weka model training, comparison, and serialization
* `evaluation` — metrics and confusion matrix
* `web` — prediction service, REST endpoint, and static UI

All three estimators consume the same sparse TF-IDF feature vectors. The
vectorizer and classifier are saved together so prediction uses the exact
training vocabulary and inverse document frequencies.
