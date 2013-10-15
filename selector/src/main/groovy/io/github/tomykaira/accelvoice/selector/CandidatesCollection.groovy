package io.github.tomykaira.accelvoice.selector

import groovy.transform.Immutable

class CandidatesCollection {
    final List<String> candidates
    private String selected;
    RecognizerLibrary recognizerLibrary = RecognizerLibrary.INSTANCE
    List<CandidatesCollection.TokenConversionMap> mapping
    Normalizer normalizer = new Normalizer(PronouncingDictionary.fromResource)
    final SelectionListener selectionListener

    def CandidatesCollection(List<String> candidates, SelectionListener selectionListener) {
        this.candidates = candidates
        this.selectionListener = selectionListener
    }

    void setRecognizerLibrary(RecognizerLibrary library) {
        this.recognizerLibrary = library
    }

    void select() {
        if (selected != null)
            return

        def unknowns = new ArrayList<String>()
        mapping = candidates.collect {
            def result = normalizer.normalize(it)
            unknowns.addAll(result.unknowns)
            new TokenConversionMap(it, result.words)
        }

        recognizerLibrary.register_cb_recognized(CallbackWrapper.callbackRecognized { result ->
            if (result >= 0 && result < mapping.size()) {
                selected = mapping[result].original
                selectionListener.notify(selected)
            }
        })
        def result = recognizerLibrary
                .start_recognition(new SplitWordList(mapping.collect { it.words }), unknowns.toArray() as String[])
        if (result < 0)
            throw new RecognitionException()
    }

    @Immutable
    private static class TokenConversionMap {
        String original
        List<String> words
    }
}
