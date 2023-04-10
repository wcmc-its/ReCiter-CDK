#!/bin/bash

# Replace <cronjob-name> with the name of your CronJob
CRONJOB_NAME="reciter-inst-client"
# Replace <namespace> with the name of your namespace
NAMESPACE="reciter"

# Function to stream logs for a pod
stream_logs() {
  POD_NAME=$1
  echo "Streaming logs for pod: ${POD_NAME}"
  kubectl -n "${NAMESPACE}" logs -f --since=10s ${POD_NAME} &
}

# Find the active job created by the CronJob
JOB_NAME=$(kubectl -n "${NAMESPACE}" get jobs -o json | jq -r ".items[] | select(.metadata.name | startswith(\"${CRONJOB_NAME}\")) | select(.status.active == 1) | .metadata.name")

# Check if a job is found
if [ -z "${JOB_NAME}" ]; then
  echo "No active job found for the specified CronJob"
  exit 1
fi

# Get current pods for the job
CURRENT_PODS=$(kubectl -n "${NAMESPACE}" get pods --selector=job-name=${JOB_NAME} -o jsonpath='{.items[*].metadata.name}')

# Stream logs for current pods
for POD_NAME in ${CURRENT_PODS}; do
  stream_logs ${POD_NAME}
done

# Continuously watch for new pods and stream logs
kubectl -n "${NAMESPACE}" get pods --selector=job-name=${JOB_NAME} --watch-only -o json | jq --unbuffered -r 'select(.type=="ADDED") | .object.metadata.name' | while read NEW_POD; do
  stream_logs ${NEW_POD}
done
