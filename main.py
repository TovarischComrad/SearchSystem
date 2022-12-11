import spacy


def main():

    # spacy.cli.download("ru_core_news_lg")
    nlp = spacy.load("ru_core_news_lg")

    # Считывание первоначального результата поиска
    f1 = open('input1.txt', encoding='utf-8', mode='r')
    lst = []
    for line in f1:
        if line != "\n":
            if line[len(line) - 1] == '\n':
                lst.append(line[:len(line) - 1])
            else:
                lst.append(line)

    # Лемматизация результатов
    lem = []
    for i in range(len(lst)):
        s = " ".join([token.lemma_ for token in nlp(lst[i]) if not token.is_punct])
        lem.append(nlp(s))

    # Считывание запросов
    f2 = open('input2.txt', encoding='utf-8', mode='r')
    s = ''
    for line in f2:
        if line != "\n":
            if line[len(line) - 1] == '\n':
                s += line[:len(line) - 1] + ' '
            else:
                s += line

    # Лемматизация запросов
    req = nlp(s)

    # Степень схожести истории запросов с результатом поиска
    p = []
    for line in lem:
        p.append(line.similarity(req))

    # Нахождение записи с максимальной степенью схожести
    mx = 0
    ind = 0
    for i in range(len(p)):
        if p[i] > mx:
            mx = p[i]
            ind = i

    f = open('output.txt', encoding='utf-8', mode='w')
    f.write(lst[ind])


if __name__ == '__main__':
    main()
