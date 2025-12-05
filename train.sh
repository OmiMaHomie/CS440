#!/bin/bash
# train.sh

echo "Starting training..."
echo "===================="

java -cp "./lib/*:." edu.bu.pas.pokemon.Train \
    edu.bu.pas.pokemon.agents.RandomAgent \
    --numCycles 1000 \
    --numTrainingGames 50 \
    --numEvalGames 10 \
    --maxBufferSize 50000 \
    --numUpdates 20 \
    --miniBatchSize 64 \
    --lr 0.0005 \
    --gamma 0.99 \
    --optimizerType adam \
    --beta1 0.9 \
    --beta2 0.999 \
    --clip 10.0 \
    --outFile ./params/model \
    --seed 42 2>&1 | tee training_log.txt

echo "Training complete!"